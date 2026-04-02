package me.yuriisoft.buildnotify.network.server.ui

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import me.yuriisoft.buildnotify.BuildNotifyBundle
import me.yuriisoft.buildnotify.security.PairingPinCalculator
import me.yuriisoft.buildnotify.security.PersistentTrustedClients
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingConstants

/**
 * Non-modal IDE dialog shown when an unknown client certificate is presented
 * during a mutual-TLS handshake.
 *
 * Replaces the former fingerprint-based approval dialog with a 6-digit PIN
 * that both sides compute independently from `SHA-256(serverFP + clientFP)`.
 * The user verifies that the PIN on screen matches the one shown on the
 * mobile device — no need to compare raw hex fingerprints.
 *
 * ## Threading
 *
 * Must be constructed and shown on the EDT.
 * [me.yuriisoft.buildnotify.security.ClientToFuTrustManager.onFirstSeen]
 * guarantees this via `invokeLater`.
 *
 * @param clientFingerprint SHA-256 fingerprint of the client certificate.
 * @param serverFingerprint SHA-256 fingerprint of the server certificate.
 * @param deviceName        Human-readable device name extracted from the
 *                          client certificate's Subject CN field.
 */
class PairingPinDialog(
    private val clientFingerprint: String,
    private val serverFingerprint: String,
    private val deviceName: String,
) : DialogWrapper(/* project = */ null, /* canBeParent = */ true) {

    private val logger = thisLogger()
    private val store: PersistentTrustedClients get() = service()

    private val pin: String =
        PairingPinCalculator.derivePin(serverFingerprint, clientFingerprint)

    init {
        title = BuildNotifyBundle.message("dialog.pairing.title")
        isModal = false
        init()
    }

    // ── DialogWrapper overrides ───────────────────────────────────────────────

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            anchor = GridBagConstraints.WEST
            weightx = 1.0
            gridx = 0
        }

        gbc.gridy = 0
        gbc.insets = JBUI.insetsBottom(8)
        panel.add(
            JBLabel(BuildNotifyBundle.message("dialog.pairing.description")),
            gbc,
        )

        gbc.gridy = 1
        gbc.insets = JBUI.insetsBottom(12)
        panel.add(
            JBLabel(BuildNotifyBundle.message("dialog.pairing.device.label", deviceName)),
            gbc,
        )

        gbc.gridy = 2
        gbc.insets = JBUI.insetsBottom(4)
        gbc.anchor = GridBagConstraints.CENTER
        gbc.fill = GridBagConstraints.NONE
        panel.add(pinLabel(), gbc)

        return panel
    }

    /**
     * Replaces the default OK/Cancel pair with **Confirm** and **Reject**.
     *
     * "Reject" is intentionally on the right to reduce accidental clicks.
     */
    override fun createActions(): Array<Action> = arrayOf(confirmAction(), rejectAction())

    override fun createCancelAction(): Action? = null

    // ── Actions ───────────────────────────────────────────────────────────────

    private fun confirmAction(): Action = dialogAction(
        text = BuildNotifyBundle.message("dialog.pairing.action.confirm"),
    ) {
        store.trust(clientFingerprint)
        logger.info("Device '$deviceName' paired successfully (PIN verified)")
        close(OK_EXIT_CODE)
    }

    private fun rejectAction(): Action = dialogAction(
        text = BuildNotifyBundle.message("dialog.pairing.action.reject"),
    ) {
        store.reject(clientFingerprint)
        logger.info("Device '$deviceName' pairing rejected")
        close(CANCEL_EXIT_CODE)
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    /**
     * Large, centered, monospaced label displaying the PIN split into two
     * groups of three for readability (e.g. "042 817").
     */
    private fun pinLabel(): JBLabel {
        val formatted = "${pin.substring(0, 3)} ${pin.substring(3)}"
        return JBLabel(formatted, SwingConstants.CENTER).apply {
            font = Font(Font.MONOSPACED, Font.BOLD, 32)
            border = JBUI.Borders.empty(12)
        }
    }

    private fun dialogAction(text: String, perform: () -> Unit): Action =
        object : DialogWrapperAction(text) {
            override fun doAction(e: java.awt.event.ActionEvent?) = perform()
        }
}
