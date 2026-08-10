package de.dragonschain.offgridmesh.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import de.dragonschain.offgridmesh.mesh.MeshManager
import de.dragonschain.offgridmesh.ui.screens.ChannelsScreen
import de.dragonschain.offgridmesh.ui.screens.MapScreen
import de.dragonschain.offgridmesh.ui.screens.SosScreen
import de.dragonschain.offgridmesh.ui.screens.StatusScreen

private sealed class Tab(val route: String, val label: String, val icon: ImageVector) {
    data object Map : Tab("map", "Karte", Icons.Default.Map)
    data object Sos : Tab("sos", "SOS", Icons.Default.Emergency)
    data object Channels : Tab("channels", "Kanäle", Icons.Default.Forum)
    data object Status : Tab("status", "Status", Icons.Default.SettingsInputAntenna)
}

private val tabs = listOf(Tab.Map, Tab.Sos, Tab.Channels, Tab.Status)

@Composable
fun AppNav(meshManager: MeshManager) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                tabs.forEach { tab ->
                    val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Tab.Map.route,
            modifier = Modifier.padding(innerPadding).padding(top = 8.dp),
        ) {
            composable(Tab.Map.route) { MapScreen(meshManager) }
            composable(Tab.Sos.route) { SosScreen(meshManager) }
            composable(Tab.Channels.route) { ChannelsScreen() }
            composable(Tab.Status.route) { StatusScreen(meshManager) }
        }
    }
}
