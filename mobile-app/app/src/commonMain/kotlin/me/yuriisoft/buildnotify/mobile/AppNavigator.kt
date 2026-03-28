package me.yuriisoft.buildnotify.mobile

import androidx.navigation.NavHostController
import me.yuriisoft.buildnotify.mobile.core.navigation.Navigator

class AppNavigator(
    private val navController: NavHostController,
) : Navigator {

    override fun navigateTo(route: String, clearBackStack: Boolean) {
        navController.navigate(route) {
            if (clearBackStack) {
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true
                }
            }
        }
    }

    override fun back() {
        navController.popBackStack()
    }
}
