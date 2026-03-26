package me.yuriisoft.buildnotify.mobile.core.communication

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ChannelResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

internal class DefaultEventCommunication<T : Any>(capacity: Int = Channel.Factory.BUFFERED) :
    EventCommunication.Mutable<T> {
    private val channel = Channel<T>(capacity)
    override val observe: Flow<T> = channel.receiveAsFlow()
    override suspend fun send(event: T) {
        channel.send(event)
    }

    override fun trySend(event: T): ChannelResult<Unit> = channel.trySend(event)
}
