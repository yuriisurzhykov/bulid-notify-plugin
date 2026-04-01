package me.yuriisoft.buildnotify.mobile.ui.theme.dimens

import androidx.compose.ui.unit.dp
import me.yuriisoft.buildnotify.mobile.ui.theme.dimens.semantic.ComponentDimensions
import me.yuriisoft.buildnotify.mobile.ui.theme.dimens.semantic.ElevationDimensions
import me.yuriisoft.buildnotify.mobile.ui.theme.dimens.semantic.IconDimensions
import me.yuriisoft.buildnotify.mobile.ui.theme.dimens.semantic.ListDimensions
import me.yuriisoft.buildnotify.mobile.ui.theme.dimens.semantic.StrokeDimensions


/* Fixed dimensions */
internal val FixedIconDimensions = IconDimensions(
    small = 16.dp,
    regular = 24.dp,
    large = 40.dp,
    xLarge = 64.dp,
    xxLarge = 96.dp,
)

internal val FixedStrokeDimensions = StrokeDimensions(
    thin = 1.dp,
    regular = 1.5.dp,
)

internal val FixedElevationDimensions = ElevationDimensions(
    none = 0.dp,
    small = 2.dp,
    medium = 4.dp,
    large = 6.dp,
)

internal val FixedListDimensions = ListDimensions(
    itemHeightSmall = 48.dp,
    itemHeightRegular = 56.dp,
    itemHeightLarge = 72.dp,
)

internal val FixedComponentDimensions = ComponentDimensions(
    fabSize = 56.dp,
    navBarHeight = 56.dp,
    avatarSize = 40.dp,
    statusDotSize = 8.dp,
    progressBarHeight = 6.dp,
    progressCircularSize = 40.dp,
    progressCircularStrokeWidth = 4.dp,
    progressSegmentGap = 2.dp,
    badgeHeight = 24.dp,
    badgePaddingHorizontal = 8.dp,
    buttonMinHeight = 48.dp,
    dotProgressDotSize = 8.dp,
    dotProgressSpacing = 6.dp,
    ringProgressSize = 120.dp,
)