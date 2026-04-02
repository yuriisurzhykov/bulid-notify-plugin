package me.yuriisoft.buildnotify.mobile.core.cache.source

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch

abstract class CachedReadableDataSource<P : Any, T : Any>(
    private val remote: ReadableDataSource<P, T>,
    private val local: MutableDataSource<P, T>,
) : ReadableDataSource<P, T> {

    override fun observe(params: P): Flow<T> = channelFlow {
        launch {
            remote.observe(params)
                // TODO: Decide what to do with the exception and how to correctly map it
                //  to something meaningful.
                .catch { error -> onRemoteError(params, error) }
                .collect { data -> local.save(params, data) }
        }
        local.observe(params).collect { data ->
            send(data)
        }
    }

    protected open fun onRemoteError(params: P, error: Throwable) {}
}
