package me.yuriisoft.buildnotify.mobile.core.communication

import kotlinx.coroutines.flow.StateFlow

interface StateCommunication<T : Any> {

    interface Observer<T : Any> {
        val observe: StateFlow<T>
    }

    interface Mutator<T : Any> {
        fun put(value: T)
        fun update(transform: (T) -> T)
    }

    interface Mutable<T : Any> : StateCommunication<T>, Observer<T>, Mutator<T>
}
