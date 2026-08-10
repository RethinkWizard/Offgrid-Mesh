package de.dragonschain.offgridmesh.mesh

import de.dragonschain.offgridmesh.data.BatteryMode
import de.dragonschain.offgridmesh.data.Message
import de.dragonschain.offgridmesh.data.Node
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Abstraktion über die Übertragungsschicht (BLE + WifiP2pManager).
 *
 * Status:
 * - `BleMeshManager` (v0.3): echte BLE-Discovery (Scan + Advertise) über
 *   BluetoothLeScanner/-Advertiser. Node-Liste ist live.
 * - `MockMeshManager`: statische Test-Daten, nützlich für reine
 *   UI-Entwicklung ohne echtes Bluetooth (z. B. im Emulator ohne BLE).
 *
 * Offen:
 * - WifiP2pManager als Fallback bei größerer Reichweite/Datenmenge
 * - Store-and-Forward über lokale Room/SQLite-Queue
 * - Echte Nachrichtenübertragung per GATT (aktuell nur lokal, siehe
 *   sendMessage/broadcastSos in BleMeshManager)
 */
interface MeshManager {
    val visibleNodes: StateFlow<List<Node>>
    val messages: StateFlow<List<Message>>

    fun startDiscovery()
    fun stopDiscovery()
    fun setBatteryMode(mode: BatteryMode)
    fun sendMessage(channelId: String, text: String)
    fun broadcastSos(lat: Double?, lon: Double?)

    /**
     * TODO v0.3: Node-Modus (Relay, "Variante 1" laut Konzept).
     * Bestehendes Altgerät wird zum Fix-Relay: kein UI-Rendering, Display
     * aus, Dauer-Discovery ohne Batteriesparmodus, Betrieb an
     * Powerbank/Solar. Läuft auf der gleichen Codebasis wie ein normaler
     * Node — kein Zusatz-Hardware nötig. Der Node leitet nur
     * verschlüsselte Pakete weiter (Ende-zu-Ende bleibt zwischen den
     * Nutzern, der Relay kann nichts mitlesen).
     *
     * fun enableNodeRelayMode()
     */
}

class MockMeshManager : MeshManager {

    private val _visibleNodes = MutableStateFlow(
        listOf(
            Node(id = "node-1", displayName = "Basti", lastSeenMillis = System.currentTimeMillis() - 4 * 60_000, hops = 1),
            Node(id = "node-2", displayName = "Lena", lastSeenMillis = System.currentTimeMillis() - 12 * 60_000, hops = 2),
        )
    )
    override val visibleNodes: StateFlow<List<Node>> = _visibleNodes.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    override val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    override fun startDiscovery() {
        // TODO: BLE-Scan + WifiP2pManager.discoverPeers() starten
    }

    override fun stopDiscovery() {
        // TODO: Scan stoppen
    }

    override fun setBatteryMode(mode: BatteryMode) {
        // TODO: Scan-Intervall anpassen (Normal: dauerhaft, Spar: 15-20s, Ultra: 60-90s)
    }

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
        // TODO: tatsächliches Dauerscan-Override + Multi-Hop-Relay an alle Nodes
    }
}
