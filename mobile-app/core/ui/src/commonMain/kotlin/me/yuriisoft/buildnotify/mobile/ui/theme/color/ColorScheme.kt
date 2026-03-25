package me.yuriisoft.buildnotify.mobile.ui.theme.color

import androidx.compose.material.Colors
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import me.yuriisoft.buildnotify.mobile.ui.theme.color.semantic.ColorRole
import me.yuriisoft.buildnotify.mobile.ui.theme.color.semantic.ContentColors
import me.yuriisoft.buildnotify.mobile.ui.theme.color.semantic.DiffColors
import me.yuriisoft.buildnotify.mobile.ui.theme.color.semantic.StatusColors
import me.yuriisoft.buildnotify.mobile.ui.theme.color.semantic.StatusRole
import me.yuriisoft.buildnotify.mobile.ui.theme.color.semantic.SurfaceColors

@Immutable
data class ColorScheme(
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

val LightColorScheme = ColorScheme(
    surface = SurfaceColors(
        background = Palette.Gray50,
        primary = Palette.White,
        elevated = Palette.DarkSurface,
        elevatedVariant = Palette.DarkCard,
        code = Palette.CodeSurface,
        nav = Palette.White,
    ),
    content = ContentColors(
        primary = Palette.Gray800,
        secondary = Palette.Gray500,
        tertiary = Palette.Gray400,
        onElevated = Palette.White,
        onElevatedVariant = Palette.Gray400,
        onCode = Palette.Gray50,
        onCodeVariant = Palette.Gray500,
    ),
    primary = ColorRole(
        main = Palette.Green500,
        variant = Palette.Green400,
        onMain = Palette.White,
        container = Palette.Green50,
        onContainer = Palette.Green700,
    ),
    secondary = ColorRole(
        main = Palette.Indigo500,
        variant = Palette.Indigo400,
        onMain = Palette.White,
        container = Palette.Indigo500,
        onContainer = Palette.Indigo500,
    ),
    tertiary = ColorRole(
        main = Palette.Lime500,
        variant = Palette.Lime400,
        onMain = Palette.Gray900,
        container = Palette.Lime500,
        onContainer = Palette.Lime500,
    ),
    highlight = Palette.Violet500,
    status = StatusColors(
        success = StatusRole(
            main = Palette.Green500,
            onMain = Palette.White,
            container = Palette.Green50,
            onContainer = Palette.Green700,
        ),
        error = StatusRole(
            main = Palette.Red500,
            onMain = Palette.White,
            container = Palette.Red50,
            onContainer = Palette.Red600,
        ),
        warning = StatusRole(
            main = Palette.Amber500,
            onMain = Palette.Gray900,
            container = Palette.Amber50,
            onContainer = Palette.Amber700,
        ),
        info = StatusRole(
            main = Palette.Blue500,
            onMain = Palette.White,
            container = Palette.Blue50,
            onContainer = Palette.Blue700,
        ),
    ),
    diff = DiffColors(
        addBackground = Palette.Green50,
        addContent = Palette.Green700,
        removeBackground = Palette.Red50,
        removeContent = Palette.Red600,
    ),
    outline = Palette.Gray300,
    divider = Palette.Gray200,
)

val DarkColorScheme = ColorScheme(
    surface = SurfaceColors(
        background = Palette.DarkContent,
        primary = Palette.CodeSurface,
        elevated = Palette.DarkSurface,
        elevatedVariant = Palette.DarkCard,
        code = Palette.CodeSurface,
        nav = Palette.DarkSurface,
    ),
    content = ContentColors(
        primary = Palette.Gray50,
        secondary = Palette.Gray400,
        tertiary = Palette.Gray500,
        onElevated = Palette.White,
        onElevatedVariant = Palette.Gray400,
        onCode = Palette.Gray50,
        onCodeVariant = Palette.Gray500,
    ),
    primary = ColorRole(
        main = Palette.Green500,
        variant = Palette.Green400,
        onMain = Palette.White,
        container = Palette.Green900,
        onContainer = Palette.Green300,
    ),
    secondary = ColorRole(
        main = Palette.Indigo500,
        variant = Palette.Indigo400,
        onMain = Palette.White,
        container = Palette.Indigo500,
        onContainer = Palette.Indigo500,
    ),
    tertiary = ColorRole(
        main = Palette.Lime500,
        variant = Palette.Lime400,
        onMain = Palette.Gray900,
        container = Palette.Lime500,
        onContainer = Palette.Lime500,
    ),
    highlight = Palette.Violet500,
    status = StatusColors(
        success = StatusRole(
            main = Palette.Green400,
            onMain = Palette.White,
            container = Palette.Green900,
            onContainer = Palette.Green300,
        ),
        error = StatusRole(
            main = Palette.Red500,
            onMain = Palette.White,
            container = Palette.Red900,
            onContainer = Palette.Red300,
        ),
        warning = StatusRole(
            main = Palette.Amber500,
            onMain = Palette.Gray900,
            container = Palette.Amber900,
            onContainer = Palette.Amber500,
        ),
        info = StatusRole(
            main = Palette.Blue500,
            onMain = Palette.White,
            container = Palette.Blue900,
            onContainer = Palette.Blue500,
        ),
    ),
    diff = DiffColors(
        addBackground = Palette.Green900,
        addContent = Palette.Green300,
        removeBackground = Palette.Red900,
        removeContent = Palette.Red300,
    ),
    outline = Palette.Gray600,
    divider = Palette.Gray700,
)

fun ColorScheme.toMaterialColors(): Colors = Colors(
    primary = primary.main,
    primaryVariant = primary.variant,
    secondary = secondary.main,
    secondaryVariant = secondary.variant,
    background = surface.background,
    surface = surface.primary,
    error = status.error.main,
    onPrimary = primary.onMain,
    onSecondary = secondary.onMain,
    onBackground = content.primary,
    onSurface = content.primary,
    onError = Palette.White,
    isLight = this === LightColorScheme,
)
