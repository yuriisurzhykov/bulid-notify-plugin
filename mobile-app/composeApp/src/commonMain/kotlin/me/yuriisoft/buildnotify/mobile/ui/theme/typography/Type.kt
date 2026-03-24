package me.yuriisoft.buildnotify.mobile.ui.theme.typography

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
data class BuildNotifyTypography(
    val displayLarge: TextStyle = TextStyle(
        fontSize = 32.sp,
        fontWeight = FontWeight.ExtraBold,
        lineHeight = 40.sp,
    ),
    val headingMedium: TextStyle = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 28.sp,
    ),
    val titleMedium: TextStyle = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 24.sp,
    ),
    val bodyMedium: TextStyle = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 20.sp,
    ),
    val labelMedium: TextStyle = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 16.sp,
    ),
    val caption: TextStyle = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 16.sp,
    ),
    val code: TextStyle = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        fontFamily = FontFamily.Monospace,
        lineHeight = 18.sp,
    ),
)
