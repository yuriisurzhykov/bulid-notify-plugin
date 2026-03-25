plugins {
    id("cmp-library")
}

android {
    namespace = "me.yuriisoft.buildnotify.mobile.core.navigation"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.jetbrains.compose.navigation)
        }
    }
}
