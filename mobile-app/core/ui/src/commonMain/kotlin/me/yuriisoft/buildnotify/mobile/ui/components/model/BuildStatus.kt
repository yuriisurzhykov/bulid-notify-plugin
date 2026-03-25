package me.yuriisoft.buildnotify.mobile.ui.components.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import me.yuriisoft.buildnotify.mobile.ui.theme.BuildNotifyTheme
import me.yuriisoft.buildnotify.mobile.ui.theme.color.semantic.StatusRole

@Immutable
sealed interface BuildStatus {
    data object Running : BuildStatus
    data object Success : BuildStatus
    data object Failed : BuildStatus
    data object Cancelled : BuildStatus
}

val BuildStatus.statusRole: StatusRole
    @Composable @ReadOnlyComposable
    get() = when (this) {
        BuildStatus.Running -> BuildNotifyTheme.colors.status.info
        BuildStatus.Success -> BuildNotifyTheme.colors.status.success
        BuildStatus.Failed -> BuildNotifyTheme.colors.status.error
        BuildStatus.Cancelled -> BuildNotifyTheme.colors.status.warning
    }
