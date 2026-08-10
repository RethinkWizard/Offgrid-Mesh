package de.dragonschain.offgridmesh.data

/**
 * Ein anderes Gerät, das im Mesh gesehen wurde.
 * lastSeenMillis = System.currentTimeMillis() beim letzten Kontakt.
 * lat/lon = letzte bekannte Position, sofern per SOS/Karte geteilt.
 */
data class Node(
    val id: String,
    val displayName: String,
    val lastSeenMillis: Long,
    val lat: Double? = null,
    val lon: Double? = null,
    val hops: Int = 1, // 1 = direkter Kontakt, >1 = über Relay erreicht
)

enum class ChannelType { FAMILY, TEAM, BROADCAST }

data class Channel(
    val id: String,
    val name: String,
    val type: ChannelType,
)

data class Message(
    val id: String,
    val channelId: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val timestampMillis: Long,
    val isSos: Boolean = false,
    val relayed: Boolean = false, // true = über andere Nodes weitergeleitet, nicht direkt empfangen
)

enum class BatteryMode { NORMAL, SAVER, ULTRA_SAVER }
