package me.yuriisoft.buildnotify.mobile

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import me.yuriisoft.buildnotify.mobile.ui.screen.AppScreen
import me.yuriisoft.buildnotify.mobile.ui.screen.BuildStatusScreen
import me.yuriisoft.buildnotify.mobile.ui.screen.DiscoveryScreen
import me.yuriisoft.buildnotify.mobile.ui.screen.HistoryScreen
import me.yuriisoft.buildnotify.mobile.ui.theme.BuildNotifyTheme

/**
 * Root composable. Owns the NavController and wires all top-level destinations.
 *
 * Called from [MainActivity] on Android and [MainViewController] on iOS,
 * making this the single CMP entry point shared across platforms.
 */
@Composable
fun App() {
    BuildNotifyTheme {
        val navController = rememberNavController()

        NavHost(
            navController = navController,
            startDestination = AppScreen.Discovery.route,
        ) {
            composable(AppScreen.Discovery.route) {
                DiscoveryScreen(
                    onHostSelected = { host, port ->
                        navController.navigate(AppScreen.BuildStatus.createRoute(host, port))
                    },
                )
            }

            composable(
                route = AppScreen.BuildStatus.route,
                arguments = listOf(
                    navArgument(AppScreen.BuildStatus.ARG_HOST) { type = NavType.StringType },
                    navArgument(AppScreen.BuildStatus.ARG_PORT) { type = NavType.IntType },
                ),
            ) { backStackEntry ->
                val host = backStackEntry.arguments?.getString(AppScreen.BuildStatus.ARG_HOST)
                    ?: return@composable
                val port = backStackEntry.arguments?.getInt(AppScreen.BuildStatus.ARG_PORT)
                    ?: return@composable
                BuildStatusScreen(
                    host = host,
                    port = port,
                    onNavigateToHistory = { navController.navigate(AppScreen.History.route) },
                )
            }

            composable(AppScreen.History.route) {
                HistoryScreen()
            }
        }
    }
}
