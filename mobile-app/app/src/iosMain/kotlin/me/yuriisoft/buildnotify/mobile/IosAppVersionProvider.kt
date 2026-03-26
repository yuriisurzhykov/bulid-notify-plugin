package me.yuriisoft.buildnotify.mobile

import me.yuriisoft.buildnotify.mobile.core.platform.AppVersionProvider
import platform.Foundation.NSBundle

/**
 * Reads the version from the iOS bundle's `CFBundleShortVersionString`.
 */
class IosAppVersionProvider : AppVersionProvider {

    override val versionName: String =
        NSBundle.mainBundle.infoDictionary
            ?.get("CFBundleShortVersionString") as? String
            ?: "unknown"
}
