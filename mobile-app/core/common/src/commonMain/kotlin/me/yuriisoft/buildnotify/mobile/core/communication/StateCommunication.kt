package me.yuriisoft.buildnotify.mobile.core.communication

import kotlinx.coroutines.flow.StateFlow

/**
 * Implements an `Observer` pattern and provides access to the reactive state. This interface is
 * an abstraction layer above the actual flows/channels. It's helpful to make the code easily
 * testable and to make the code more readable.
 *
 * The main responsibility of the [StateCommunication] is to manipulate with the state, or to
 * provide a clean way to access the [StateFlow].
 *
 * The `StateCommunication` is a marker interface for smart contract. It holds the [Observer] and
 * [Mutator] interfaces for read and write operations. The [Mutable] interface is the combination
 * of both, and you can think of these interfaces this way:
 * - [Observer] -> read only
 * - [Mutator] -> write only
 * - [Mutable] -> read and write
 * */
interface StateCommunication<T : Any> {

    interface Observer<T : Any> : StateCommunication<T> {
        val observe: StateFlow<T>
    }

    interface Mutator<T : Any> : StateCommunication<T> {
        fun put(value: T)
        fun update(transform: (T) -> T)
    }

    interface Mutable<T : Any> : StateCommunication<T>, Observer<T>, Mutator<T>
}
