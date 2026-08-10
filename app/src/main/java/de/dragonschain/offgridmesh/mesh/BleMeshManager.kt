package de.dragonschain.offgridmesh.mesh

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import de.dragonschain.offgridmesh.data.BatteryMode
import de.dragonschain.offgridmesh.data.Message
import de.dragonschain.offgridmesh.data.Node
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

private const val TAG = "BleMeshManager"

// Fester, eigener Service-UUID für OFFGRID MESH — nur Geräte mit dieser
// UUID im Advertising werden als Mesh-Node erkannt, kein wildes BLE-Rauschen.
private val SERVICE_UUID: UUID = UUID.fromString("8f5a1e2c-8d3b-4c9a-9e1f-6b7c2d4a5e10")

// Nach dieser Zeit ohne neues Advertisement gilt ein Node als "weg"
// und wird aus der Liste entfernt.
private const val NODE_TIMEOUT_MS = 5 * 60_000L
private const val CLEANUP_INTERVAL_MS = 30_000L

/**
 * Echte BLE-Discovery über BluetoothLeScanner + BluetoothLeAdvertiser.
 *
 * Aktueller Umfang (v0.3): Geräte finden sich gegenseitig, Node-Liste ist
 * live und echt. Nachrichtenversand (`sendMessage`, `broadcastSos`) läuft
 * noch lokal — echte Datenübertragung per GATT folgt mit der
 * Store-and-Forward-Queue (nächster Schritt).
 *
 * WICHTIG: Die Node-ID basiert aktuell auf der Bluetooth-MAC-Adresse.
 * Ab Android 8 wird diese bei vielen Geräten periodisch randomisiert
 * (Privacy-Feature) — ein Node kann daher nach einiger Zeit als "neuer"
 * Node erscheinen. Wird in einem späteren Schritt durch eine stabile,
 * selbst vergebene ID in den Service-Daten des Advertisements gelöst.
 */
class BleMeshManager(context: Context) : MeshManager {

    private val appContext = context.applicationContext
    private val bluetoothManager =
        appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val adapter: BluetoothAdapter? = bluetoothManager?.adapter
    private val scanner: BluetoothLeScanner? get() = adapter?.bluetoothLeScanner
    private val advertiser: BluetoothLeAdvertiser? get() = adapter?.bluetoothLeAdvertiser

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var cleanupJob: Job? = null
    private var ultraSaverJob: Job? = null
    private var currentMode: BatteryMode = BatteryMode.NORMAL
    private var isDiscoveryRunning = false

    private val nodesById = mutableMapOf<String, Node>()
    private val _visibleNodes = MutableStateFlow<List<Node>>(emptyList())
    override val visibleNodes: StateFlow<List<Node>> = _visibleNodes.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    override val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    /**
     * true, wenn Bluetooth-Hardware vorhanden und eingeschaltet ist.
     * MainActivity sollte das vor startDiscovery() prüfen und ggf. den
     * System-Dialog zum Einschalten anzeigen (ACTION_REQUEST_ENABLE).
     */
    fun isBluetoothReady(): Boolean = adapter?.isEnabled == true

    @SuppressLint("MissingPermission") // Aufrufer (MainActivity) prüft Runtime-Permissions vorher
    override fun startDiscovery() {
        if (isDiscoveryRunning) return
        val bleScanner = scanner
        val bleAdvertiser = advertiser
        if (bleScanner == null || bleAdvertiser == null) {
            Log.w(TAG, "Bluetooth LE nicht verfügbar oder ausgeschaltet")
            return
        }

        val advertiseSettings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .setConnectable(false)
            .build()

        val advertiseData = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(SERVICE_UUID))
            .setIncludeDeviceName(true)
            .build()

        try {
            bleAdvertiser.startAdvertising(advertiseSettings, advertiseData, advertiseCallback)
        } catch (e: SecurityException) {
            Log.w(TAG, "Advertising ohne Berechtigung nicht möglich", e)
        }

        startScan(bleScanner, currentMode)

        isDiscoveryRunning = true
        cleanupJob = scope.launch { runCleanupLoop() }
    }

    @SuppressLint("MissingPermission")
    override fun stopDiscovery() {
        if (!isDiscoveryRunning) return
        try {
            advertiser?.stopAdvertising(advertiseCallback)
            scanner?.stopScan(scanCallback)
        } catch (e: SecurityException) {
            Log.w(TAG, "Stoppen ohne Berechtigung nicht möglich", e)
        }
        cleanupJob?.cancel()
        ultraSaverJob?.cancel()
        isDiscoveryRunning = false
    }

    @SuppressLint("MissingPermission")
    override fun setBatteryMode(mode: BatteryMode) {
        currentMode = mode
        ultraSaverJob?.cancel()
        if (!isDiscoveryRunning) return

        val bleScanner = scanner ?: return
        try {
            bleScanner.stopScan(scanCallback)
        } catch (e: SecurityException) {
            Log.w(TAG, "Scan-Stop ohne Berechtigung nicht möglich", e)
        }
        startScan(bleScanner, mode)
    }

    @SuppressLint("MissingPermission")
    private fun startScan(bleScanner: BluetoothLeScanner, mode: BatteryMode) {
        val filters = listOf(
            ScanFilter.Builder().setServiceUuid(ParcelUuid(SERVICE_UUID)).build()
        )

        when (mode) {
            BatteryMode.NORMAL -> {
                val settings = ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build()
                try {
                    bleScanner.startScan(filters, settings, scanCallback)
                } catch (e: SecurityException) {
                    Log.w(TAG, "Scan ohne Berechtigung nicht möglich", e)
                }
            }
            BatteryMode.SAVER -> {
                val settings = ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                    .build()
                try {
                    bleScanner.startScan(filters, settings, scanCallback)
                } catch (e: SecurityException) {
                    Log.w(TAG, "Scan ohne Berechtigung nicht möglich", e)
                }
            }
            BatteryMode.ULTRA_SAVER -> {
                // Kein Dauerscan: kurze Bursts alle 60-90 Sek., dazwischen
                // komplett aus — das spart am meisten Akku.
                ultraSaverJob = scope.launch { runUltraSaverScanLoop(bleScanner, filters) }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun runUltraSaverScanLoop(bleScanner: BluetoothLeScanner, filters: List<ScanFilter>) {
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()
        while (scope.isActive && currentMode == BatteryMode.ULTRA_SAVER) {
            try {
                bleScanner.startScan(filters, settings, scanCallback)
            } catch (e: SecurityException) {
                Log.w(TAG, "Scan ohne Berechtigung nicht möglich", e)
            }
            delay(10_000L) // 10 Sek. scannen
            try {
                bleScanner.stopScan(scanCallback)
            } catch (e: SecurityException) {
                // ignorieren, Scan lief evtl. schon nicht mehr
            }
            delay(75_000L) // 75 Sek. Pause -> insgesamt ~alle 85 Sek. ein Burst
        }
    }

    private suspend fun runCleanupLoop() {
        while (scope.isActive) {
            delay(CLEANUP_INTERVAL_MS)
            val now = System.currentTimeMillis()
            val before = nodesById.size
            nodesById.entries.removeAll { (_, node) -> now - node.lastSeenMillis > NODE_TIMEOUT_MS }
            if (nodesById.size != before) {
                _visibleNodes.value = nodesById.values.sortedByDescending { it.lastSeenMillis }
            }
        }
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartFailure(errorCode: Int) {
            Log.w(TAG, "Advertising fehlgeschlagen, Code: $errorCode")
        }
    }

    @SuppressLint("MissingPermission") // device.name-Zugriff, Permission wird vor startDiscovery geprüft
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val id = device.address ?: return
            val name = try {
                device.name
            } catch (e: SecurityException) {
                null
            } ?: "Unbekannt (${id.takeLast(5)})"

            val existing = nodesById[id]
            nodesById[id] = Node(
                id = id,
                displayName = name,
                lastSeenMillis = System.currentTimeMillis(),
                lat = existing?.lat,
                lon = existing?.lon,
                hops = 1, // direkter BLE-Kontakt
            )
            _visibleNodes.value = nodesById.values.sortedByDescending { it.lastSeenMillis }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.w(TAG, "Scan fehlgeschlagen, Code: $errorCode")
        }
    }

    // --- Nachrichtenversand: noch lokal, echte Übertragung folgt mit GATT ---

    override fun sendMessage(channelId: String, text: String) {
        val msg = Message(
            id = "msg-${System.currentTimeMillis()}",
            channelId = channelId,
            senderId = "self",
            senderName = "Ich",
            text = text,
            timestampMillis = System.currentTimeMillis(),
        )
        _messages.value = _messages.value + msg
        // TODO v0.4: GATT-Verbindung zu Nodes aufbauen und Nachricht
        // tatsächlich übertragen + in Store-and-Forward-Queue puffern.
    }

    override fun broadcastSos(lat: Double?, lon: Double?) {
        val msg = Message(
            id = "sos-${System.currentTimeMillis()}",
            channelId = "broadcast",
            senderId = "self",
            senderName = "Ich",
            text = "SOS — Notfall-Broadcast${if (lat != null && lon != null) " ($lat, $lon)" else ""}",
            timestampMillis = System.currentTimeMillis(),
            isSos = true,
        )
        _messages.value = _messages.value + msg
        // TODO v0.4: Dauerscan-Override + tatsächliches Multi-Hop-Relay per GATT
    }
}
