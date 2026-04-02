package me.yuriisoft.buildnotify.mobile.core.cache.source

interface MutableDataSource<P, T> : ReadableDataSource<P, T>, WritableDataSource<P, T>
