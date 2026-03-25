package me.yuriisoft.buildnotify.mobile.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import me.yuriisoft.buildnotify.mobile.ui.components.button.Fab
import me.yuriisoft.buildnotify.mobile.ui.components.button.IconButton
import me.yuriisoft.buildnotify.mobile.ui.components.button.PrimaryButton
import me.yuriisoft.buildnotify.mobile.ui.components.button.SecondaryButton
import me.yuriisoft.buildnotify.mobile.ui.components.foundation.Badge
import me.yuriisoft.buildnotify.mobile.ui.components.foundation.Divider
import me.yuriisoft.buildnotify.mobile.ui.components.foundation.StatusDot
import me.yuriisoft.buildnotify.mobile.ui.components.foundation.Text
import me.yuriisoft.buildnotify.mobile.ui.components.icon.Icon
import me.yuriisoft.buildnotify.mobile.ui.components.icon.StatusIcon
import me.yuriisoft.buildnotify.mobile.ui.components.layout.CodeSurface
import me.yuriisoft.buildnotify.mobile.ui.components.layout.ElevatedSurface
import me.yuriisoft.buildnotify.mobile.ui.components.layout.Surface
import me.yuriisoft.buildnotify.mobile.ui.components.progress.CircularProgress
import me.yuriisoft.buildnotify.mobile.ui.components.progress.LinearProgress
import me.yuriisoft.buildnotify.mobile.ui.components.progress.mode.circular.CircularProgressMode
import me.yuriisoft.buildnotify.mobile.ui.components.progress.mode.linear.LinearProgressMode
import me.yuriisoft.buildnotify.mobile.ui.resource.RawText
import me.yuriisoft.buildnotify.mobile.ui.resource.VectorImage
import me.yuriisoft.buildnotify.mobile.ui.theme.BuildNotifyTheme

@Composable
fun CatalogScreen(modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BuildNotifyTheme.colors.surface.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        SectionHeader("Typography")
        TypographySection()

        SectionHeader("Buttons")
        ButtonsSection()

        SectionHeader("Icon & StatusIcon")
        IconsSection()

        SectionHeader("StatusDot")
        StatusDotsSection()

        SectionHeader("Badge")
        BadgeSection()

        SectionHeader("Divider")
        DividerSection()

        SectionHeader("Progress — Linear")
        LinearProgressSection()

        SectionHeader("Progress — Circular")
        CircularProgressSection()

        SectionHeader("Surfaces")
        SurfacesSection()

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = RawText(title),
        style = BuildNotifyTheme.typography.headingMedium,
        color = BuildNotifyTheme.colors.content.primary,
    )
    Divider()
}

@Composable
private fun TypographySection() {
    val typography = BuildNotifyTheme.typography
    val items = listOf(
        "displayLarge" to typography.displayLarge,
        "displayMedium" to typography.displayMedium,
        "displaySmall" to typography.displaySmall,
        "headingLarge" to typography.headingLarge,
        "headingMedium" to typography.headingMedium,
        "headingSmall" to typography.headingSmall,
        "titleLarge" to typography.titleLarge,
        "titleMedium" to typography.titleMedium,
        "titleSmall" to typography.titleSmall,
        "bodyLarge" to typography.bodyLarge,
        "bodyMedium" to typography.bodyMedium,
        "bodySmall" to typography.bodySmall,
        "labelLarge" to typography.labelLarge,
        "labelMedium" to typography.labelMedium,
        "labelSmall" to typography.labelSmall,
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { (name, style) ->
            Text(
                text = RawText(name),
                style = style,
                color = BuildNotifyTheme.colors.content.primary,
            )
        }
        Divider()
        Text(
            text = RawText("code.regular: val x = 42"),
            style = typography.code.regular,
            color = BuildNotifyTheme.colors.content.primary,
        )
        Text(
            text = RawText("code.small: fun main() {}"),
            style = typography.code.small,
            color = BuildNotifyTheme.colors.content.secondary,
        )
    }
}

@Composable
private fun ButtonsSection() {
    val icon = VectorImage(PlusIcon)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PrimaryButton(onClick = {}) {
                Text(
                    text = RawText("Primary"),
                    style = BuildNotifyTheme.typography.labelLarge,
                )
            }
            PrimaryButton(onClick = {}, enabled = false) {
                Text(
                    text = RawText("Disabled"),
                    style = BuildNotifyTheme.typography.labelLarge,
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SecondaryButton(onClick = {}) {
                Text(
                    text = RawText("Secondary"),
                    style = BuildNotifyTheme.typography.labelLarge,
                )
            }
            SecondaryButton(onClick = {}, enabled = false) {
                Text(
                    text = RawText("Disabled"),
                    style = BuildNotifyTheme.typography.labelLarge,
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Fab(image = icon, onClick = {})
            IconButton(image = icon, onClick = {})
            IconButton(image = icon, onClick = {}, enabled = false)
        }
    }
}

@Composable
private fun IconsSection() {
    val checkIcon = VectorImage(CheckIcon)
    val closeIcon = VectorImage(CloseIcon)
    val infoIcon = VectorImage(InfoIcon)
    val status = BuildNotifyTheme.colors.status

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(image = checkIcon, contentDescription = null)
            Icon(image = closeIcon, contentDescription = null)
            Icon(image = infoIcon, contentDescription = null)
            Icon(
                image = checkIcon,
                contentDescription = null,
                tint = BuildNotifyTheme.colors.primary.main,
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusIcon(
                containerColor = status.success.container,
                contentColor = status.success.onContainer,
                image = checkIcon,
            )
            StatusIcon(
                containerColor = status.error.container,
                contentColor = status.error.onContainer,
                image = closeIcon,
            )
            StatusIcon(
                containerColor = status.warning.container,
                contentColor = status.warning.onContainer,
                image = infoIcon,
            )
            StatusIcon(
                containerColor = status.info.container,
                contentColor = status.info.onContainer,
                loading = true,
            )
        }
    }
}

@Composable
private fun StatusDotsSection() {
    val status = BuildNotifyTheme.colors.status
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusDot(color = status.success.main)
        Text(
            text = RawText("Success"),
            style = BuildNotifyTheme.typography.bodySmall,
            color = BuildNotifyTheme.colors.content.secondary,
        )
        StatusDot(color = status.error.main)
        Text(
            text = RawText("Error"),
            style = BuildNotifyTheme.typography.bodySmall,
            color = BuildNotifyTheme.colors.content.secondary,
        )
        StatusDot(color = status.warning.main)
        Text(
            text = RawText("Warning"),
            style = BuildNotifyTheme.typography.bodySmall,
            color = BuildNotifyTheme.colors.content.secondary,
        )
        StatusDot(color = status.info.main)
        Text(
            text = RawText("Info"),
            style = BuildNotifyTheme.typography.bodySmall,
            color = BuildNotifyTheme.colors.content.secondary,
        )
    }
}

@Composable
private fun BadgeSection() {
    val status = BuildNotifyTheme.colors.status
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Badge(
            containerColor = status.success.container,
            contentColor = status.success.onContainer,
        ) {
            Text(
                text = RawText("SUCCESS"),
                style = BuildNotifyTheme.typography.labelSmall,
            )
        }
        Badge(
            containerColor = status.error.container,
            contentColor = status.error.onContainer,
        ) {
            Text(
                text = RawText("FAILED"),
                style = BuildNotifyTheme.typography.labelSmall,
            )
        }
        Badge(
            containerColor = status.warning.container,
            contentColor = status.warning.onContainer,
        ) {
            Text(
                text = RawText("WARNING"),
                style = BuildNotifyTheme.typography.labelSmall,
            )
        }
        Badge(
            containerColor = status.info.container,
            contentColor = status.info.onContainer,
        ) {
            Text(
                text = RawText("RUNNING"),
                style = BuildNotifyTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun DividerSection() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Divider()
        Divider(color = BuildNotifyTheme.colors.primary.main)
    }
}

@Composable
private fun LinearProgressSection() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        LinearProgress(mode = LinearProgressMode.Determinate(progress = 0.6f))
        LinearProgress(mode = LinearProgressMode.Indeterminate())
        LinearProgress(mode = LinearProgressMode.Buffered(progress = 0.4f, bufferProgress = 0.7f))
        LinearProgress(mode = LinearProgressMode.Stepped(progress = 0.5f, steps = 5))
        LinearProgress(mode = LinearProgressMode.Striped(progress = 0.65f))
        LinearProgress(mode = LinearProgressMode.Pulsing())
    }
}

@Composable
private fun CircularProgressSection() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgress(mode = CircularProgressMode.Determinate(progress = 0.7f))
            CircularProgress(mode = CircularProgressMode.Indeterminate())
            CircularProgress(mode = CircularProgressMode.Segmented(progress = 0.6f, segments = 6))
            CircularProgress(mode = CircularProgressMode.Pulsing())
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgress(mode = CircularProgressMode.Countdown(progress = 0.75f))
            Text(
                text = RawText("Countdown / MultiRing"),
                style = BuildNotifyTheme.typography.bodyMedium,
                color = BuildNotifyTheme.colors.content.secondary,
            )
        }
    }
}

@Composable
private fun SurfacesSection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(Modifier.padding(16.dp)) {
                Text(
                    text = RawText("Surface (primary)"),
                    style = BuildNotifyTheme.typography.bodyMedium,
                )
            }
        }

        ElevatedSurface(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(Modifier.padding(16.dp)) {
                Text(
                    text = RawText("ElevatedSurface"),
                    style = BuildNotifyTheme.typography.bodyMedium,
                )
            }
        }

        CodeSurface(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(Modifier.padding(16.dp)) {
                Text(
                    text = RawText("CodeSurface — val greeting = \"Hello\""),
                    style = BuildNotifyTheme.typography.code.regular,
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            onClick = {},
        ) {
            Box(Modifier.padding(16.dp)) {
                Text(
                    text = RawText("Surface (clickable)"),
                    style = BuildNotifyTheme.typography.bodyMedium,
                )
            }
        }
    }
}

// region Mini vector icons for the catalog (no material-icons dependency)

private val PlusIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Plus",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            fill = SolidColor(Color.Black),
        ) {
            moveTo(11f, 5f)
            lineTo(13f, 5f)
            lineTo(13f, 11f)
            lineTo(19f, 11f)
            lineTo(19f, 13f)
            lineTo(13f, 13f)
            lineTo(13f, 19f)
            lineTo(11f, 19f)
            lineTo(11f, 13f)
            lineTo(5f, 13f)
            lineTo(5f, 11f)
            lineTo(11f, 11f)
            close()
        }
    }.build()
}

private val CheckIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Check",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            fill = SolidColor(Color.Black),
        ) {
            moveTo(9f, 16.17f)
            lineTo(4.83f, 12f)
            lineTo(3.41f, 13.41f)
            lineTo(9f, 19f)
            lineTo(21f, 7f)
            lineTo(19.59f, 5.59f)
            close()
        }
    }.build()
}

private val CloseIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Close",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            fill = SolidColor(Color.Black),
        ) {
            moveTo(19f, 6.41f)
            lineTo(17.59f, 5f)
            lineTo(12f, 10.59f)
            lineTo(6.41f, 5f)
            lineTo(5f, 6.41f)
            lineTo(10.59f, 12f)
            lineTo(5f, 17.59f)
            lineTo(6.41f, 19f)
            lineTo(12f, 13.41f)
            lineTo(17.59f, 19f)
            lineTo(19f, 17.59f)
            lineTo(13.41f, 12f)
            close()
        }
    }.build()
}

private val InfoIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Info",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            fill = SolidColor(Color.Black),
            pathFillType = PathFillType.EvenOdd,
        ) {
            moveTo(12f, 2f)
            arcTo(10f, 10f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12f, 22f)
            arcTo(10f, 10f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12f, 2f)
            close()
            moveTo(11f, 7f)
            lineTo(13f, 7f)
            lineTo(13f, 9f)
            lineTo(11f, 9f)
            close()
            moveTo(11f, 11f)
            lineTo(13f, 11f)
            lineTo(13f, 17f)
            lineTo(11f, 17f)
            close()
        }
    }.build()
}

// endregion
