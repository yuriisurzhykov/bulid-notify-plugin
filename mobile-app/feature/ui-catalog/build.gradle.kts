plugins {
    id("cmp-library")
    alias(libs.plugins.ksp)
}

android {
    namespace = "me.yuriisoft.buildnotify.mobile.feature.catalog"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(project(":core:navigation"))
            implementation(project(":core:ui"))
            implementation(libs.lifecycle.viewmodel)
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.kotlin.inject.runtime)
        }
    }
}

dependencies {
    add("kspCommonMainMetadata", libs.kotlin.inject.compiler)
    add("kspAndroid", libs.kotlin.inject.compiler)
}
