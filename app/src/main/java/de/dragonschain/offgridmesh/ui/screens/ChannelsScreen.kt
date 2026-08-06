package de.dragonschain.offgridmesh.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import de.dragonschain.offgridmesh.data.Channel
import de.dragonschain.offgridmesh.data.ChannelType

private val defaultChannels = listOf(
    Channel(id = "family", name = "Familie", type = ChannelType.FAMILY),
    Channel(id = "team", name = "Team", type = ChannelType.TEAM),
    Channel(id = "broadcast", name = "Broadcast-Allgemein", type = ChannelType.BROADCAST),
)

@Composable
fun ChannelsScreen(onChannelSelected: (Channel) -> Unit = {}) {
    Column {
        defaultChannels.forEach { channel ->
            ChannelRow(channel = channel, onClick = { onChannelSelected(channel) })
            HorizontalDivider(color = MaterialTheme.colorScheme.surface)
        }
    }
}

@Composable
private fun ChannelRow(channel: Channel, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = iconFor(channel.type),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface, CircleShape)
                .padding(10.dp),
        )
        Text(
            text = channel.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

private fun iconFor(type: ChannelType): ImageVector = when (type) {
    ChannelType.FAMILY -> Icons.Default.People
    ChannelType.TEAM -> Icons.Default.Groups
    ChannelType.BROADCAST -> Icons.Default.Campaign
}
