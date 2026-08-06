package de.dragonschain.offgridmesh.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.dragonschain.offgridmesh.mesh.MeshManager
import kotlinx.coroutines.delay

private const val HOLD_DURATION_MS = 3000L

@Composable
fun SosScreen(meshManager: MeshManager) {
    var isHolding by remember { mutableStateOf(false) }
    var isSent by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "sosProgress")

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Text(
            text = if (isSent) "SOS AKTIV — wird gesendet" else "3 Sekunden gedrückt halten für Notfall-Broadcast",
            style = MaterialTheme.typography.bodyLarge,
            color = if (isSent) Color(0xFFD32F2F) else MaterialTheme.colorScheme.onBackground,
            fontWeight = if (isSent) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(bottom = 32.dp),
        )

        Box(
            modifier = Modifier
                .size(220.dp)
                .background(
                    color = if (isSent) Color(0xFF8E0000) else Color(0xFFD32F2F),
                    shape = CircleShape,
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isHolding = true
                            var elapsed = 0L
                            while (isHolding && elapsed < HOLD_DURATION_MS) {
                                delay(50)
                                elapsed += 50
                                progress = elapsed / HOLD_DURATION_MS.toFloat()
                            }
                            if (isHolding && elapsed >= HOLD_DURATION_MS) {
                                isSent = true
                                meshManager.broadcastSos(lat = null, lon = null) // TODO: echte GPS-Position (FusedLocationProvider)
                            }
                            val released = tryAwaitRelease()
                            isHolding = false
                            if (!isSent) progress = 0f
                        }
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "SOS",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
        }

        Text(
            text = "Fortschritt: ${(animatedProgress * 100).toInt()}%",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 24.dp),
        )
    }
}
