package me.yuriisoft.buildnotify.mobile

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import me.yuriisoft.buildnotify.mobile.core.platform.INetworkMonitor

/**
 * Android [INetworkMonitor] backed by [ConnectivityManager].
 *
 * Only WiFi and Ethernet transports are tracked because mDNS discovery
 * requires a local network — cellular connections cannot reach LAN services.
 *
 * The implementation uses [callbackFlow] to bridge the callback-based
 * [ConnectivityManager.NetworkCallback] into a [StateFlow]. The flow is
 * started eagerly so that the first read never suspends.
 */
class AndroidNetworkMonitor(
    context: Context,
) : INetworkMonitor {

    private val connectivityManager: ConnectivityManager? =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    override val isNetworkAvailable: StateFlow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(hasMatchingNetwork())
            }
        }

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
            .build()

        connectivityManager?.registerNetworkCallback(request, callback)

        awaitClose {
            connectivityManager?.unregisterNetworkCallback(callback)
        }
    }.stateIn(
        scope = CoroutineScope(Dispatchers.Default),
        started = SharingStarted.Eagerly,
        initialValue = hasMatchingNetwork(),
    )

    /**
     * Synchronous snapshot used for the initial [StateFlow] value and for
     * re-evaluating availability after a single network is lost (another
     * qualifying network may still be active).
     */
    private fun hasMatchingNetwork(): Boolean {
        val capabilities = connectivityManager
            ?.getNetworkCapabilities(connectivityManager.activeNetwork)
            ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }
}
