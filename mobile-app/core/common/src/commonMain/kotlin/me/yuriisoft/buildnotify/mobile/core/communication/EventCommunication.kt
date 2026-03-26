package me.yuriisoft.buildnotify.mobile.core.communication

import kotlinx.coroutines.channels.ChannelResult
import kotlinx.coroutines.flow.Flow

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
