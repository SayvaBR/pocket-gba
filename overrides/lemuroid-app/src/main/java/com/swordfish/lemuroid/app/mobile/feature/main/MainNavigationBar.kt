package com.swordfish.lemuroid.app.mobile.feature.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideogameAsset
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController

private data class PocketDestination(
    val label: String,
    val route: String,
    val selected: ImageVector,
    val unselected: ImageVector,
    val active: (MainRoute?) -> Boolean,
)

private val destinations = listOf(
    PocketDestination("Início", MainRoute.HOME.route, Icons.Filled.Home, Icons.Outlined.Home) { it == MainRoute.HOME },
    PocketDestination("Biblioteca", "systems/GBA", Icons.Filled.VideogameAsset, Icons.Outlined.VideogameAsset) {
        it == MainRoute.SYSTEM_GAMES || it == MainRoute.SYSTEMS
    },
    PocketDestination("Buscar", MainRoute.SEARCH.route, Icons.Filled.Search, Icons.Outlined.Search) { it == MainRoute.SEARCH },
    PocketDestination("Ajustes", MainRoute.SETTINGS.route, Icons.Filled.Settings, Icons.Outlined.Settings) { it?.root == MainRoute.SETTINGS },
)

@Composable
fun MainNavigationBar(currentRoute: MainRoute?, navController: NavHostController) {
    // Sub-pages retain normal Android back navigation instead of becoming extra tabs.
    if (currentRoute != null && currentRoute.parent != null && currentRoute != MainRoute.SYSTEM_GAMES) return

    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
            Row(
                modifier = Modifier.fillMaxWidth().height(74.dp).padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                destinations.forEach { destination ->
                    val active = destination.active(currentRoute)
                    val foreground = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    Column(
                        modifier = Modifier.weight(1f).clickable {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = false }
                                launchSingleTop = true
                                restoreState = false
                            }
                        }.padding(vertical = 9.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(
                            imageVector = if (active) destination.selected else destination.unselected,
                            contentDescription = destination.label,
                            tint = foreground,
                        )
                        Text(
                            text = destination.label,
                            color = foreground,
                            fontSize = 11.sp,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
