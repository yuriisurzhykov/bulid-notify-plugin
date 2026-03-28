package me.yuriisoft.buildnotify.mobile.core.communication

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * A default, thread-safe implementation of [StateCommunication.Mutable] that utilizes
 * [MutableStateFlow] as the underlying mechanism for state management.
 *
 * This class serves as a concrete communication bridge, typically used within ViewModels
 * or similar components to manage a UI state. It encapsulates the private mutable state
 * and exposes a read-only [StateFlow] through the [observe] property to ensure
 * unidirectional data flow.
 *
 * @param T The type of the state object. Must be non-nullable.
 */
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
