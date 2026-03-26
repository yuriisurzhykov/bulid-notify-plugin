package me.yuriisoft.buildnotify.mobile.testing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.yuriisoft.buildnotify.mobile.core.platform.INetworkMonitor

/**
 * In-memory fake of [INetworkMonitor] for unit tests.
 *
 * Call [setAvailable] to simulate network connectivity changes.
 * Defaults to [initiallyAvailable] (true if not specified).
 */
class FakeNetworkMonitor(
    initiallyAvailable: Boolean = true,
) : INetworkMonitor {

    private val _isNetworkAvailable = MutableStateFlow(initiallyAvailable)
    override val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()

    fun setAvailable(available: Boolean) {
        _isNetworkAvailable.value = available
    }
}
