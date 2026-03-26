package me.yuriisoft.buildnotify.mobile

import me.yuriisoft.buildnotify.mobile.core.platform.AppVersionProvider

/**
 * Reads the version name from the generated [BuildConfig].
 *
 * Requires `buildConfig = true` in the app-level `build.gradle.kts`.
 */
class AndroidAppVersionProvider : AppVersionProvider {
    override val versionName: String = BuildConfig.VERSION_NAME
}
