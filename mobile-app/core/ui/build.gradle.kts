plugins {
    id("cmp-library")
}

android {
    namespace = "me.yuriisoft.buildnotify.mobile.core.ui"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.jetbrains.compose.material)
            implementation(libs.jetbrains.compose.resources)
        }
    }
}
