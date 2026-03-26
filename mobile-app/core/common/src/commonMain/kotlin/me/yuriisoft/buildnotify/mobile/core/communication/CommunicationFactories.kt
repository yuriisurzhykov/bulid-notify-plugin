@file:Suppress("FunctionName")

package me.yuriisoft.buildnotify.mobile.core.communication

fun <T : Any> StateCommunication(initial: T): StateCommunication.Mutable<T> =
    DefaultStateCommunication(initial)

fun <T : Any> EventCommunication(): EventCommunication.Mutable<T> =
    DefaultEventCommunication()
