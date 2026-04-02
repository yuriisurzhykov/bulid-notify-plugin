package me.yuriisoft.buildnotify.mobile.core.cache.db

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

actual class CacheDatabaseFactory(private val context: Context) {

    actual fun create(): CacheDatabase =
        CacheDatabase(AndroidSqliteDriver(CacheDatabase.Schema, context, "build_notify_cache.db"))
}
