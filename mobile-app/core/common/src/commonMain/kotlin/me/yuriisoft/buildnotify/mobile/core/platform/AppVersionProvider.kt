package me.yuriisoft.buildnotify.mobile.core.platform

/**
 * Provides the display version of the application.
 *
 * Platform implementations live in `:app` androidMain / iosMain and are
 * passed into the DI graph via the [AppComponent] constructor.
 */
interface AppVersionProvider {
    val versionName: String
}
