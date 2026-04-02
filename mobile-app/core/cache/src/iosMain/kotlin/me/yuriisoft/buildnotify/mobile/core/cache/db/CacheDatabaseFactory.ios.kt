package me.yuriisoft.buildnotify.mobile.core.cache.db

import app.cash.sqldelight.driver.native.NativeSqliteDriver

actual class CacheDatabaseFactory {

    actual fun create(): CacheDatabase =
        CacheDatabase(NativeSqliteDriver(CacheDatabase.Schema, "build_notify_cache.db"))
}
