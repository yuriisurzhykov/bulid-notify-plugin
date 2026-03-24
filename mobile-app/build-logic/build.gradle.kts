plugins {
    `kotlin-dsl`
}

dependencies {
    // Gradle plugin artifacts needed to compile convention plugins
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.android.gradlePlugin)
}
