package me.yuriisoft.buildnotify.mobile.ui.theme.typography

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import build_notify_mobile.core.ui.generated.resources.Res
import build_notify_mobile.core.ui.generated.resources.inter_black
import build_notify_mobile.core.ui.generated.resources.inter_bold
import build_notify_mobile.core.ui.generated.resources.inter_extrabold
import build_notify_mobile.core.ui.generated.resources.inter_medium
import build_notify_mobile.core.ui.generated.resources.inter_regular
import build_notify_mobile.core.ui.generated.resources.inter_semibold
import build_notify_mobile.core.ui.generated.resources.inter_thin
import build_notify_mobile.core.ui.generated.resources.jetbrains_mono_regular
import org.jetbrains.compose.resources.Font

@Composable
@SuppressLint("ComposableNaming")
fun InterFontFamily(): FontFamily = FontFamily(
    Font(Res.font.inter_thin, FontWeight.Thin),
    Font(Res.font.inter_regular, FontWeight.Normal),
    Font(Res.font.inter_medium, FontWeight.Medium),
    Font(Res.font.inter_semibold, FontWeight.SemiBold),
    Font(Res.font.inter_bold, FontWeight.Bold),
    Font(Res.font.inter_extrabold, FontWeight.ExtraBold),
    Font(Res.font.inter_black, FontWeight.Black),
)

@Composable
@SuppressLint("ComposableNaming")
fun JetBrainsMonoFontFamily(): FontFamily = FontFamily(
    Font(Res.font.jetbrains_mono_regular, FontWeight.Normal),
)
