package me.yuriisoft.buildnotify.mobile

import android.app.Application
import me.yuriisoft.buildnotify.mobile.feature.discovery.data.discovery.AndroidNsdDiscovery

/**
 * Application-scoped entry point that hosts the DI [component].
 *
 * The component is created once and shared across:
 *   - [MainActivity] — for Compose UI (screens set)
 *   - [BuildMonitorService][me.yuriisoft.buildnotify.mobile.service.BuildMonitorService]
 *     — for [IConnectionRepository] observation
 *
 * Declared in `AndroidManifest.xml` via `android:name`.
 */
class BuildNotifyApp : Application() {

    lateinit var component: AppComponent
        private set

    override fun onCreate() {
        super.onCreate()
        component = AppComponent::class.create(
            AndroidNsdDiscovery(applicationContext),
            AndroidNetworkMonitor(applicationContext),
            AndroidAppVersionProvider(),
        )
    }
}
