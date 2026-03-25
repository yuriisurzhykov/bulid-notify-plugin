package me.yuriisoft.buildnotify.mobile.ui.theme.dimens

import androidx.compose.runtime.Immutable
import me.yuriisoft.buildnotify.mobile.ui.theme.dimens.semantic.ComponentDimensions
import me.yuriisoft.buildnotify.mobile.ui.theme.dimens.semantic.ElevationDimensions
import me.yuriisoft.buildnotify.mobile.ui.theme.dimens.semantic.IconDimensions
import me.yuriisoft.buildnotify.mobile.ui.theme.dimens.semantic.LayoutDimensions
import me.yuriisoft.buildnotify.mobile.ui.theme.dimens.semantic.ListDimensions
import me.yuriisoft.buildnotify.mobile.ui.theme.dimens.semantic.SpacingDimensions
import me.yuriisoft.buildnotify.mobile.ui.theme.dimens.semantic.StrokeDimensions

@Immutable
data class BuildNotifyDimensions(
    val spacing: SpacingDimensions,
    val layout: LayoutDimensions,
    val icon: IconDimensions,
    val stroke: StrokeDimensions,
    val elevation: ElevationDimensions,
    val list: ListDimensions,
    val component: ComponentDimensions,
)

// region Top-level breakpoint instances
val CompactDimensions = BuildNotifyDimensions(
    spacing = CompactSpacing,
    layout = CompactLayout,
    icon = FixedIconDimensions,
    stroke = FixedStrokeDimensions,
    elevation = FixedElevationDimensions,
    list = FixedListDimensions,
    component = FixedComponentDimensions,
)

val MediumDimensions = BuildNotifyDimensions(
    spacing = MediumSpacing,
    layout = MediumLayout,
    icon = FixedIconDimensions,
    stroke = FixedStrokeDimensions,
    elevation = FixedElevationDimensions,
    list = FixedListDimensions,
    component = FixedComponentDimensions,
)

val ExpandedDimensions = BuildNotifyDimensions(
    spacing = ExpandedSpacing,
    layout = ExpandedLayout,
    icon = FixedIconDimensions,
    stroke = FixedStrokeDimensions,
    elevation = FixedElevationDimensions,
    list = FixedListDimensions,
    component = FixedComponentDimensions,
)
