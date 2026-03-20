package me.yuriisoft.buildnotify.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindIntText
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.openapi.components.service
import me.yuriisoft.buildnotify.BuildNotifyBundle
import me.yuriisoft.buildnotify.network.discovery.MdnsAdvertiser
import me.yuriisoft.buildnotify.network.server.BuildWebSocketServer
import javax.swing.JComponent

/**
 * Settings UI rendered in Settings → Tools → Build Notify.
 *
 * Uses Kotlin UI DSL v2 — built into the IntelliJ Platform, zero extra dependencies,
 * inherits the current IDE Look & Feel (Light / Darcula / High Contrast) automatically.
 *
 * Jewel/Compose was intentionally dropped: it requires complex bundledModule wiring
 * for AS builds and is not officially supported for third-party plugins.
 */
class PluginSettingsConfigurable : Configurable {

    private val settings: PluginSettingsState
        get() = service()

    private var panel: DialogPanel? = null
    private val uiState = PluginSettingsState.State()

    override fun getDisplayName(): String =
        BuildNotifyBundle.message("plugin.display.name")

    override fun createComponent(): JComponent {
        loadUiState()

        return panel {
            group(BuildNotifyBundle.message("settings.group.server")) {
                row(BuildNotifyBundle.message("settings.field.port")) {
                    intTextField(range = 1024..65535)
                        .bindIntText(uiState::port)
                    comment(BuildNotifyBundle.message("settings.field.port.comment"))
                }
                row(BuildNotifyBundle.message("settings.field.service.name")) {
                    textField()
                        .bindText(uiState::serviceName)
                    comment(BuildNotifyBundle.message("settings.field.service.name.comment"))
                }
                row(BuildNotifyBundle.message("settings.field.keystore.path")) {
                    textFieldWithBrowseButton(
                        fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFileDescriptor()
                            .withTitle(BuildNotifyBundle.message("settings.field.keystore.browse.title")),
                        project = ProjectManager.getInstance().defaultProject,
                        fileChosen = { it.path },
                    )
                        .bindText(uiState::keystorePath)
                        .align(AlignX.FILL)
                        .resizableColumn()
                    comment(BuildNotifyBundle.message("settings.field.keystore.path.comment"))
                }
            }

            group(BuildNotifyBundle.message("settings.group.connection")) {
                row(BuildNotifyBundle.message("settings.field.heartbeat.interval")) {
                    intTextField(range = 5..300)
                        .bindIntText(uiState::heartbeatIntervalSec)
                    comment(BuildNotifyBundle.message("settings.field.heartbeat.interval.comment"))
                }
                row(BuildNotifyBundle.message("settings.field.connection.lost.timeout")) {
                    intTextField(range = 5..300)
                        .bindIntText(uiState::connectionLostTimeoutSec)
                    comment(BuildNotifyBundle.message("settings.field.connection.lost.timeout.comment"))
                }
                row(BuildNotifyBundle.message("settings.field.session.timeout")) {
                    intTextField(range = 1..24 * 60)
                        .bindIntText(uiState::sessionTimeoutMinutes)
                    comment(BuildNotifyBundle.message("settings.field.session.timeout.comment"))
                }
            }

            group(BuildNotifyBundle.message("settings.group.notifications")) {
                row {
                    checkBox(BuildNotifyBundle.message("settings.field.send.warnings"))
                        .bindSelected(uiState::sendWarnings)
                }
                row(BuildNotifyBundle.message("settings.field.max.issues")) {
                    intTextField(range = 1..100)
                        .bindIntText(uiState::maxIssuesPerNotification)
                    comment(BuildNotifyBundle.message("settings.field.max.issues.comment"))
                }
            }
        }.also { panel = it }
    }

    override fun isModified(): Boolean = panel?.isModified() ?: false

    override fun apply() {
        panel?.apply()
        val previous = settings.snapshot()
        settings.loadState(uiState.copy())
        val updated = settings.snapshot()

        val serverNeedsRestart =
            previous.port != updated.port ||
                previous.connectionLostTimeoutSec != updated.connectionLostTimeoutSec ||
                previous.keystorePath != updated.keystorePath
        val mdnsNeedsRestart =
            previous.port != updated.port ||
                previous.serviceName != updated.serviceName

        if (serverNeedsRestart || mdnsNeedsRestart) {
            val server = service<BuildWebSocketServer>()
            val mdns = service<MdnsAdvertiser>()
            if (serverNeedsRestart) server.stop()
            if (mdnsNeedsRestart) mdns.stop()
            if (serverNeedsRestart) server.start()
            if (mdnsNeedsRestart) mdns.start()
        }
    }

    override fun reset() {
        loadUiState()
        panel?.reset()
    }

    private fun loadUiState() {
        val snapshot = settings.snapshot()
        uiState.port = snapshot.port
        uiState.serviceName = snapshot.serviceName
        uiState.sendWarnings = snapshot.sendWarnings
        uiState.maxIssuesPerNotification = snapshot.maxIssuesPerNotification
        uiState.heartbeatIntervalSec = snapshot.heartbeatIntervalSec
        uiState.connectionLostTimeoutSec = snapshot.connectionLostTimeoutSec
        uiState.sessionTimeoutMinutes = snapshot.sessionTimeoutMinutes
        uiState.keystorePath = snapshot.keystorePath
    }
}