package me.yuriisoft.buildnotify.mobile.feature.discovery.data.discovery

/**
 * Thrown when [android.net.nsd.NsdManager] fails to start service discovery.
 *
 * [errorCode] maps to the `NsdManager.FAILURE_*` constants
 * (e.g. `FAILURE_INTERNAL_ERROR`, `FAILURE_ALREADY_ACTIVE`, `FAILURE_MAX_LIMIT`).
 */
class NsdDiscoveryException(
    val errorCode: Int,
) : Exception("NSD discovery failed with error code $errorCode")
