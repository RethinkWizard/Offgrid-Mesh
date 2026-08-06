package de.dragonschain.offgridmesh.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.dragonschain.offgridmesh.data.Node
import de.dragonschain.offgridmesh.mesh.MeshManager

@Composable
fun MapScreen(meshManager: MeshManager) {
    val nodes by meshManager.visibleNodes.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        if (nodes.isEmpty()) {
            Text(
                text = "Keine Nodes in Reichweite",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(nodes) { node -> NodeCard(node) }
            }
        }
    }
}

@Composable
private fun NodeCard(node: Node) {
    Card(
        modifier = Modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = node.displayName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            val minutesAgo = ((System.currentTimeMillis() - node.lastSeenMillis) / 60_000).toInt()
            Text(
                text = "zuletzt gesehen vor $minutesAgo Min · ${node.hops} Hop(s)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (node.lat != null && node.lon != null) {
                Row {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("${node.lat}, ${node.lon}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun Row(content: @Composable () -> Unit) =
    androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically, content = content)
