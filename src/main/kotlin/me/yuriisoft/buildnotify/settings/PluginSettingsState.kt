package me.yuriisoft.buildnotify.settings

import com.intellij.openapi.components.*

@Service(Service.Level.APP)
@State(
    name = "BuildNotifySettings",
    storages = [Storage("buildNotifySettings.xml", roamingType = RoamingType.DISABLED)]
)
class PluginSettingsState : PersistentStateComponent<PluginSettingsState.State> {

    data class State(
        var port: Int = 8765,
        var serviceName: String = "AndroidStudio-BuildNotify",
        var sendWarnings: Boolean = true,
        var maxIssuesPerNotification: Int = 20,
        var heartbeatIntervalSec: Int = 30,
        var connectionLostTimeoutSec: Int = 30,
        /** Drop build sessions with no finish event after this many minutes. */
        var sessionTimeoutMinutes: Int = 30,
        /** Optional absolute path to a JKS keystore; empty = try classpath `keystore.jks` or env `BUILDNOTIFY_KEYSTORE_PATH`. */
        var keystorePath: String = "",
    )

    private var _state: State = State()

    fun snapshot() = _state.copy()

    override fun getState(): State = _state

    override fun loadState(state: State) {
        _state = state
    }
}