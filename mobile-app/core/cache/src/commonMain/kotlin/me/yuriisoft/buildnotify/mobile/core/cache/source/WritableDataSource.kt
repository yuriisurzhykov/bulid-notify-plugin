package me.yuriisoft.buildnotify.mobile.core.cache.source

interface WritableDataSource<P, in T> {

    /** Persists [data] associated with [params]. */
    suspend fun save(params: P, data: T)

    /** Removes data associated with [params]. */
    suspend fun delete(params: P)
}
