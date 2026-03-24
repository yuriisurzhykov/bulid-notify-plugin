package me.yuriisoft.buildnotify.mobile.ui.screen

/**
 * Typed navigation destinations for the app.
 *
 * Using a sealed class keeps the route strings co-located with
 * any required arguments, making typos impossible at call sites.
 */
sealed class AppScreen(val route: String) {

    data object Discovery : AppScreen("discovery")

    data object BuildStatus : AppScreen("build_status/{host}/{port}") {
        const val ARG_HOST = "host"
        const val ARG_PORT = "port"

        fun createRoute(host: String, port: Int): String = "build_status/$host/$port"
    }

    data object History : AppScreen("history")
}
