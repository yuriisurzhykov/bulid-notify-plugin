package me.yuriisoft.buildnotify.mobile.feature.discovery.presentation

import androidx.compose.runtime.Immutable

@Immutable
sealed interface DiscoveryEvent {

    data object NavigateToBuild : DiscoveryEvent
}
