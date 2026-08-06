package de.dragonschain.offgridmesh.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.dragonschain.offgridmesh.data.BatteryMode
import de.dragonschain.offgridmesh.mesh.MeshManager
import androidx.compose.foundation.layout.Row

@Composable
fun StatusScreen(meshManager: MeshManager) {
    val nodes by meshManager.visibleNodes.collectAsState()
    var selectedMode by remember { mutableStateOf(BatteryMode.NORMAL) }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text(
            text = "Akku-Modus",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Column(modifier = Modifier.selectableGroup().padding(top = 8.dp)) {
            ModeRow(
                label = "Normal — Dauerscan",
                selected = selectedMode == BatteryMode.NORMAL,
                onSelect = { selectedMode = BatteryMode.NORMAL; meshManager.setBatteryMode(BatteryMode.NORMAL) },
            )
            ModeRow(
                label = "Spar — alle 15–20 Sek.",
                selected = selectedMode == BatteryMode.SAVER,
                onSelect = { selectedMode = BatteryMode.SAVER; meshManager.setBatteryMode(BatteryMode.SAVER) },
            )
            ModeRow(
                label = "Ultra-Spar — alle 60–90 Sek.",
                selected = selectedMode == BatteryMode.ULTRA_SAVER,
                onSelect = { selectedMode = BatteryMode.ULTRA_SAVER; meshManager.setBatteryMode(BatteryMode.ULTRA_SAVER) },
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp), color = MaterialTheme.colorScheme.surface)

        InfoLine(label = "Nodes in Reichweite", value = nodes.size.toString())
        InfoLine(label = "Reichweite (typisch)", value = "BLE: 30–100 m · WiFi-Direct: bis 200 m")
    }
}

@Composable
private fun ModeRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
    }
}
