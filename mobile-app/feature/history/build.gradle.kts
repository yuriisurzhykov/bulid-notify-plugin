plugins {
    id("cmp-library")
    alias(libs.plugins.ksp)
}

android {
    namespace = "me.yuriisoft.buildnotify.mobile.feature.history"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:domain"))
            implementation(project(":core:navigation"))
            implementation(project(":core:ui"))
            implementation(libs.lifecycle.viewmodel)
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.kotlin.inject.runtime)
        }

        commonTest.dependencies {
            implementation(project(":core:testing"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

dependencies {
    add("kspCommonMainMetadata", libs.kotlin.inject.compiler)
    add("kspAndroid", libs.kotlin.inject.compiler)
}
