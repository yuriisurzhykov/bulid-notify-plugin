plugins {
    id("cmp-library")
}

android {
    namespace = "me.yuriisoft.buildnotify.mobile.feature.networkstatus"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.toast)
            implementation(projects.core.network)
            implementation(projects.core.navigation)
            implementation(projects.core.ui)
        }

        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(kotlin("test"))
        }
    }
}
