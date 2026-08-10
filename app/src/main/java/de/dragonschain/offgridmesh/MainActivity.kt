package de.dragonschain.offgridmesh

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import de.dragonschain.offgridmesh.mesh.BleMeshManager
import de.dragonschain.offgridmesh.ui.nav.AppNav
import de.dragonschain.offgridmesh.ui.theme.OffgridMeshTheme

/**
 * Kein Onboarding, kein Login, kein Setup-Wizard.
 * App öffnen -> Berechtigungen abfragen (nur beim ersten Mal) -> direkt
 * einsatzbereit.
 */
class MainActivity : ComponentActivity() {

    private val meshManager by lazy { BleMeshManager(this) }

    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            if (results.values.all { it }) {
                ensureBluetoothEnabledThenStart()
            }
            // Bei Ablehnung: App bleibt nutzbar, Karte zeigt einfach keine
            // Nodes. Kein aufdringlicher erneuter Dialog (TODO: Hinweis auf
            // Status-Screen "Berechtigungen fehlen" statt stillschweigend).
        }

    private val requestEnableBluetooth =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (meshManager.isBluetoothReady()) {
                meshManager.startDiscovery()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OffgridMeshTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNav(meshManager = meshManager)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (hasAllRequiredPermissions()) {
            ensureBluetoothEnabledThenStart()
        } else {
            requestPermissions.launch(requiredPermissions())
        }
    }

    override fun onStop() {
        super.onStop()
        meshManager.stopDiscovery()
    }

    private fun ensureBluetoothEnabledThenStart() {
        if (meshManager.isBluetoothReady()) {
            meshManager.startDiscovery()
        } else {
            requestEnableBluetooth.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }
    }

    private fun requiredPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
            )
        } else {
            // Vor Android 12 braucht BLE-Scan Standortberechtigung
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    private fun hasAllRequiredPermissions(): Boolean =
        requiredPermissions().all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
}
