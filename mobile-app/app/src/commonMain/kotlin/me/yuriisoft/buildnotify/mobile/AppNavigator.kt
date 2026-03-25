package me.yuriisoft.buildnotify.mobile

import androidx.navigation.NavHostController
import me.yuriisoft.buildnotify.mobile.core.navigation.Navigator

class AppNavigator(
    private val navController: NavHostController,
) : Navigator {

    override fun navigateTo(route: String) {
        navController.navigate(route)
    }

    override fun back() {
        navController.popBackStack()
    }
}
