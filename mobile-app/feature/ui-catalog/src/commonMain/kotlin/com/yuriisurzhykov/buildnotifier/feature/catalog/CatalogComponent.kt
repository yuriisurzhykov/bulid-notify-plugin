package com.yuriisurzhykov.buildnotifier.feature.catalog

import me.tatarka.inject.annotations.IntoSet
import me.tatarka.inject.annotations.Provides
import me.yuriisoft.buildnotify.mobile.core.navigation.Screen

/**
 * Dependency injection component for the Catalog feature.
 *
 * This component is responsible for providing and configuring dependencies related to the
 * catalog module, specifically contributing the [CatalogScreen] to the application's
 */
interface CatalogComponent {

    @IntoSet
    @Provides
    fun catalogScreen(screen: CatalogScreen): Screen = screen
}