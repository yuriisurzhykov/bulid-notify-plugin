package me.yuriisoft.buildnotify.mobile.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import me.yuriisoft.buildnotify.mobile.ui.theme.semantic.ColorRole
import me.yuriisoft.buildnotify.mobile.ui.theme.semantic.ContentColors
import me.yuriisoft.buildnotify.mobile.ui.theme.semantic.DiffColors
import me.yuriisoft.buildnotify.mobile.ui.theme.semantic.StatusColors
import me.yuriisoft.buildnotify.mobile.ui.theme.semantic.SurfaceColors

@Immutable
data class BuildNotifyColorScheme(
    val surface: SurfaceColors,
    val content: ContentColors,
    val primary: ColorRole,
    val secondary: ColorRole,
    val tertiary: ColorRole,
    val highlight: Color,
    val status: StatusColors,
    val diff: DiffColors,
    val outline: Color,
    val divider: Color,
)