package me.yuriisoft.buildnotify.mobile.core.navigation

interface Navigator {

    fun navigateTo(route: String, clearBackStack: Boolean)

    fun back()
}
