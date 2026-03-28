package me.yuriisoft.buildnotify.mobile.feature.buildstatus.di

import me.tatarka.inject.annotations.IntoSet
import me.tatarka.inject.annotations.Provides
import me.yuriisoft.buildnotify.mobile.core.navigation.Screen
import me.yuriisoft.buildnotify.mobile.feature.buildstatus.presentation.BuildStatusScreen

interface BuildStatusComponent {

    @IntoSet
    @Provides
    fun buildStatusScreen(screen: BuildStatusScreen): Screen = screen
}