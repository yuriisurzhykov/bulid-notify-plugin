package me.yuriisoft.buildnotify.mobile.core.navigation

import androidx.navigation.NamedNavArgument
import androidx.navigation.NavDeepLink

abstract class Destination(val route: String) {

    open val arguments: List<NamedNavArgument> = emptyList()

    open val deepLinks: List<NavDeepLink> = emptyList()
}
