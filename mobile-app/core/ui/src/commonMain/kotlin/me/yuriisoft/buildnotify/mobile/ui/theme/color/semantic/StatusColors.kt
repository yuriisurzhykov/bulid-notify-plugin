package me.yuriisoft.buildnotify.mobile.ui.theme.color.semantic

import androidx.compose.runtime.Immutable

@Immutable
data class StatusColors(
    val success: StatusRole,
    val error: StatusRole,
    val warning: StatusRole,
    val info: StatusRole,
)