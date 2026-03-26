package me.yuriisoft.buildnotify.mobile.core.communication

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal class DefaultStateCommunication<T : Any>(initial: T) : StateCommunication.Mutable<T> {
    private val flow = MutableStateFlow(initial)
    override val observe: StateFlow<T> = flow.asStateFlow()
    override fun put(value: T) {
        flow.value = value
    }

    override fun update(transform: (T) -> T) {
        flow.update(transform)
    }
}
