package me.yuriisoft.buildnotify.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkTheme = Colors(
    primary = Color(0xFFBB86FC),
    primaryVariant = Color(0xFFBB86FC),
    secondary = Color(0xFFBB86FC),
    secondaryVariant = Color(0xFFBB86FC),
    background = Color(0xFFBB86FC),
    surface = Color(0xFFBB86FC),
    error = Color(0xFFBB86FC),
    onPrimary = Color(0xFFBB86FC),
    onSecondary = Color(0xFFBB86FC),
    onBackground = Color(0xFFBB86FC),
    onSurface = Color(0xFFBB86FC),
    onError = Color(0xFFBB86FC),
    isLight = false
)

private val LightTheme = Colors(
    primary = Color(0xFFBB86FC),
    primaryVariant = Color(0xFFBB86FC),
    secondary = Color(0xFFBB86FC),
    secondaryVariant = Color(0xFFBB86FC),
    background = Color(0xFFBB86FC),
    surface = Color(0xFFBB86FC),
    error = Color(0xFFBB86FC),
    onPrimary = Color(0xFFBB86FC),
    onSecondary = Color(0xFFBB86FC),
    onBackground = Color(0xFFBB86FC),
    onSurface = Color(0xFFBB86FC),
    onError = Color(0xFFBB86FC),
    isLight = true
)

@Composable
fun BuildNotifyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colors = if (darkTheme) DarkTheme else LightTheme,
        content = content,
    )
}
