plugins {
    id("kmp-library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "me.yuriisoft.buildnotify.mobile.core.data"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
        }
    }
}
