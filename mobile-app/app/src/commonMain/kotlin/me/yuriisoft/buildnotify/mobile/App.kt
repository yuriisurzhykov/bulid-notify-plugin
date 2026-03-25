package me.yuriisoft.buildnotify.mobile

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import me.yuriisoft.buildnotify.mobile.core.navigation.DiscoveryDestination
import me.yuriisoft.buildnotify.mobile.core.navigation.Screen
import me.yuriisoft.buildnotify.mobile.ui.theme.BuildNotifyTheme

@Composable
fun App(screens: Set<Screen>) {
    BuildNotifyTheme {
        val navController = rememberNavController()

        AppNavGraph(
            navController = navController,
            screens = screens,
            startRoute = DiscoveryDestination.route,
        )
    }
}
