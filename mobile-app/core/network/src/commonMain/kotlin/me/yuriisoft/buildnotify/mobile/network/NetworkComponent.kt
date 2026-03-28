package me.yuriisoft.buildnotify.mobile.network

import kotlinx.serialization.json.Json
import me.tatarka.inject.annotations.Provides
import me.yuriisoft.buildnotify.mobile.network.client.HttpClientProvider
import me.yuriisoft.buildnotify.mobile.network.connection.ManagedConnection
import me.yuriisoft.buildnotify.mobile.network.error.ErrorMapping
import me.yuriisoft.buildnotify.mobile.network.error.ErrorRecognizer
import me.yuriisoft.buildnotify.mobile.network.error.recognizers.HandshakeErrors
import me.yuriisoft.buildnotify.mobile.network.error.recognizers.LostConnectionErrors
import me.yuriisoft.buildnotify.mobile.network.error.recognizers.RefusedErrors
import me.yuriisoft.buildnotify.mobile.network.error.recognizers.TimeoutErrors
import me.yuriisoft.buildnotify.mobile.network.reconnection.ExponentialBackoff
import me.yuriisoft.buildnotify.mobile.network.reconnection.ReconnectionStrategy
import me.yuriisoft.buildnotify.mobile.network.transport.PayloadCodec
import me.yuriisoft.buildnotify.mobile.network.transport.Transport
import me.yuriisoft.buildnotify.mobile.network.transport.WebSocketTransport

/**
 * Contribution interface for the network layer.
 *
 * Encapsulates all WebSocket/transport wiring so the parent
 * component never touches [ManagedConnection] internals.
 */
interface NetworkComponent {

    @Provides
    fun payloadCodec(json: Json): PayloadCodec = PayloadCodec(json)

    @Provides
    fun transport(
        clientProvider: HttpClientProvider,
        codec: PayloadCodec,
    ): Transport = WebSocketTransport(clientProvider, codec)

    @Provides
    fun reconnectionStrategy(
        errorMapping: ErrorMapping,
    ): ReconnectionStrategy = ExponentialBackoff(errorMapping = errorMapping)

    @Provides
    fun errorRecognizers(): List<ErrorRecognizer> = listOf(
        TimeoutErrors(),
        RefusedErrors(),
        HandshakeErrors(),
        LostConnectionErrors(),
    )

    @Provides
    fun errorMapping(recognizers: List<ErrorRecognizer>): ErrorMapping =
        ErrorMapping(recognizers)
}