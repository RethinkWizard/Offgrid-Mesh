package de.dragonschain.offgridmesh

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import de.dragonschain.offgridmesh.mesh.MockMeshManager
import de.dragonschain.offgridmesh.ui.nav.AppNav
import de.dragonschain.offgridmesh.ui.theme.OffgridMeshTheme

/**
 * Kein Onboarding, kein Login, kein Setup-Wizard.
 * App öffnen -> direkt einsatzbereit. Der Name wird beim ersten
 * Senden einer Nachricht abgefragt (folgt in v0.2), nicht vorher.
 */
class MainActivity : ComponentActivity() {

    // TODO v0.2: durch BleWifiDirectMeshManager ersetzen (Dependency Injection
    // oder simple Factory reicht hier, kein Overengineering nötig)
    private val meshManager = MockMeshManager()

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
        meshManager.startDiscovery()
    }

    override fun onStop() {
        super.onStop()
        meshManager.stopDiscovery()
    }
}
