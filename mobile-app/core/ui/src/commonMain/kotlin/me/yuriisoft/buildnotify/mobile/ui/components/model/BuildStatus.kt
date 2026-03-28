package me.yuriisoft.buildnotify.mobile.ui.components.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import me.yuriisoft.buildnotify.mobile.ui.theme.BuildNotifyTheme
import me.yuriisoft.buildnotify.mobile.ui.theme.color.semantic.StatusRole

@Immutable
sealed interface BuildStatus {

    @get:[Composable ReadOnlyComposable]
    val statusRole: StatusRole

    data object Running : BuildStatus {
        override val statusRole: StatusRole
            @[Composable ReadOnlyComposable]
            get() = BuildNotifyTheme.colors.status.info
    }

    data object Success : BuildStatus {
        override val statusRole: StatusRole
            @[Composable ReadOnlyComposable]
            get() = BuildNotifyTheme.colors.status.success
    }

    data object Failed : BuildStatus {
        override val statusRole: StatusRole
            @[Composable ReadOnlyComposable]
            get() = BuildNotifyTheme.colors.status.error
    }

    data object Cancelled : BuildStatus {
        override val statusRole: StatusRole
            @[Composable ReadOnlyComposable]
            get() = BuildNotifyTheme.colors.status.warning
    }
}