package me.yuriisoft.buildnotify.mobile

import me.tatarka.inject.annotations.Scope

/**
 * Application-level scope for kotlin-inject.
 *
 * Providers annotated with this scope inside an [AppScope]-annotated
 * component become singletons for the lifetime of that component instance.
 * Use for objects that must be shared across the entire app (e.g.
 * [ManagedConnection][me.yuriisoft.buildnotify.mobile.network.connection.ManagedConnection]).
 */
@Scope
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
annotation class AppScope
