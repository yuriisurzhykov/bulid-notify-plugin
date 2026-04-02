plugins {
    id("kmp-library")
}

android {
    namespace = "me.yuriisoft.buildnotify.mobile.core.cache"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
