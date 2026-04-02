package me.yuriisoft.buildnotify.mobile.core.cache.source

import kotlinx.coroutines.flow.Flow

interface ReadableDataSource<P, out T> {

    /**
     * Returns a Flow that emits data for the given [params].
     * The Flow is typically long-lived and re-emits whenever the
     * underlying data changes.
     */
    fun observe(params: P): Flow<T>
}
