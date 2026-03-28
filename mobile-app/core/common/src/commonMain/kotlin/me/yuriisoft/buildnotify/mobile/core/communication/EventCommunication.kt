package me.yuriisoft.buildnotify.mobile.core.communication

import kotlinx.coroutines.channels.ChannelResult
import kotlinx.coroutines.flow.Flow

/**
 * Implements an `Observer` pattern and provides access to the reactive events. This interface is
 * an abstraction layer above the actual flows/channels. It's helpful to make the code easily
 * testable and to make the code more readable.
 *
 * The main responsibility of the [EventCommunication] is to provide a clean source of single shot
 * events within the application.
 *
 * The `StateCommunication` is a marker interface for smart contract. It holds the [Observer] and
 * [Emitter] interfaces for read and write operations. The [Mutable] interface is the combination
 * of both, and you can think of these interfaces this way:
 * - [Observer] -> read only
 * - [Emitter]  -> publish only
 * - [Mutable]  -> read and write
 * */

interface EventCommunication {

    interface Observer<T : Any> {
        val observe: Flow<T>
    }

    interface Emitter<T : Any> {
        suspend fun send(event: T)
        fun trySend(event: T): ChannelResult<Unit>
    }

    interface Mutable<T : Any> : Observer<T>, Emitter<T>
}
