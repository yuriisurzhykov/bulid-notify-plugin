package me.yuriisoft.buildnotify.mobile.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry

abstract class Screen {

    abstract val destination: Destination

    open val transitions: ScreenTransitions = ScreenTransitions.None

    @Composable
    abstract fun Content(
        backStackEntry: NavBackStackEntry,
        navigator: Navigator,
    )
}
