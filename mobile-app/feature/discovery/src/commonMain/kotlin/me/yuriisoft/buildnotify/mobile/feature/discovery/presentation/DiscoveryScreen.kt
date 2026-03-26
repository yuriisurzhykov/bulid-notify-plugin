package me.yuriisoft.buildnotify.mobile.feature.discovery.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import me.tatarka.inject.annotations.Inject
import me.yuriisoft.buildnotify.mobile.core.navigation.BuildStatusDestination
import me.yuriisoft.buildnotify.mobile.core.navigation.Navigator
import me.yuriisoft.buildnotify.mobile.core.navigation.Screen
import me.yuriisoft.buildnotify.mobile.core.navigation.ScreenTransitions
import me.yuriisoft.buildnotify.mobile.core.platform.AppVersionProvider
import me.yuriisoft.buildnotify.mobile.feature.discovery.domain.model.DiscoveredHost
import me.yuriisoft.buildnotify.mobile.feature.discovery.ui.DiscoveryContent

@Inject
@Immutable
class DiscoveryScreen(
    private val viewModelFactory: () -> DiscoveryViewModel,
    private val appVersionProvider: AppVersionProvider,
) : Screen() {

    override val destination = DiscoveryDestination
    override val transitions = ScreenTransitions.Fade

    @Composable
    override fun Content(backStackEntry: NavBackStackEntry, navigator: Navigator) {
        val vm by rememberUpdatedState(viewModel { viewModelFactory() })
        val state by vm.uiState.collectAsState()

        LaunchedEffect(Unit) {
            vm.uiEvents.collect { event ->
                when (event) {
                    is DiscoveryEvent.NavigateToBuild -> {
                        val route = BuildStatusDestination.createRoute(event.host, event.port)
                        navigator.navigateTo(route)
                    }
                }
            }
        }

        val onHostSelected = remember(vm) {
            { discoveredHost: DiscoveredHost -> vm.selectHost(discoveredHost) }
        }

        val onRetry = remember(vm) {
            { vm.retry() }
        }

        val onCancel = remember(vm) {
            { vm.cancel() }
        }

        val onStartScan = remember(vm) {
            { vm.retry() }
        }

        DiscoveryContent(
            state = state,
            appVersion = appVersionProvider.versionName,
            onHostSelected = onHostSelected,
            onRetry = onRetry,
            onCancel = onCancel,
            onStartScan = onStartScan,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DiscoveryScreen

        if (viewModelFactory != other.viewModelFactory) return false
        if (transitions != other.transitions) return false

        return true
    }

    override fun hashCode(): Int {
        var result = viewModelFactory.hashCode()
        result = 31 * result + transitions.hashCode()
        return result
    }
}
