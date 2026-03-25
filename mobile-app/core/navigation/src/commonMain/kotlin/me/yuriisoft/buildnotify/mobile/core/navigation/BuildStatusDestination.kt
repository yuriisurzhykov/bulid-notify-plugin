package me.yuriisoft.buildnotify.mobile.core.navigation

import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.navArgument

object BuildStatusDestination : Destination(route = "build_status/{host}/{port}") {

    override val arguments: List<NamedNavArgument> = listOf(
        navArgument("host") { type = NavType.StringType },
        navArgument("port") { type = NavType.IntType },
    )

    fun createRoute(host: String, port: Int): String = "build_status/$host/$port"
}
