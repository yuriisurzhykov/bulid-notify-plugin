package me.yuriisoft.buildnotify.mobile.feature.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import me.yuriisoft.buildnotify.mobile.core.dispatchers.AppDispatchers
import me.yuriisoft.buildnotify.mobile.core.usecase.FlowUseCase
import me.yuriisoft.buildnotify.mobile.core.usecase.NoParams
import me.yuriisoft.buildnotify.mobile.domain.model.DiscoveredHost

class DiscoveryViewModel(
    private val observeHosts: FlowUseCase<NoParams, List<DiscoveredHost>>,
    private val dispatchers: AppDispatchers,
) : ViewModel() {

    private val _state = MutableStateFlow<DiscoveryUiState>(DiscoveryUiState.Loading)
    val state: StateFlow<DiscoveryUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<DiscoveryEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<DiscoveryEvent> = _events.asSharedFlow()

    init {
        startDiscovery()
    }

    fun selectHost(host: DiscoveredHost) {
        viewModelScope.launch {
            _events.emit(DiscoveryEvent.NavigateToBuild(host.host, host.port))
        }
    }

    private fun startDiscovery() {
        dispatchers.launchBackground(viewModelScope) {
            observeHosts(NoParams)
                .catch { e -> _state.value = DiscoveryUiState.Error(e.message.orEmpty()) }
                .collect { hosts -> _state.value = DiscoveryUiState.Content(hosts) }
        }
    }
}
