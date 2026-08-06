package de.dragonschain.offgridmesh.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = AccentGreen,
    secondary = SosRed,
    background = Charcoal,
    surface = CharcoalLight,
    onPrimary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = SosRed,
)

// Bewusst nur ein dunkles Theme: bessere Lesbarkeit im Feld/Nachts,
// geringerer Akkuverbrauch bei OLED-Displays.
@Composable
fun OffgridMeshTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = MaterialTheme.typography,
        content = content,
    )
}
