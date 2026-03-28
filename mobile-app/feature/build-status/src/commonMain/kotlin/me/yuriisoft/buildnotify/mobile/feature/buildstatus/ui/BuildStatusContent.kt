package me.yuriisoft.buildnotify.mobile.feature.buildstatus.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.yuriisoft.buildnotify.mobile.ui.components.foundation.Text
import me.yuriisoft.buildnotify.mobile.ui.components.layout.ElevatedSurface
import me.yuriisoft.buildnotify.mobile.ui.resource.textResource

@Composable
fun BuildStatusContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        ElevatedSurface(
            modifier = modifier
                .fillMaxWidth(0.7f)
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                modifier = Modifier,
                text = textResource("Build Status screen"),
            )
        }
    }
}