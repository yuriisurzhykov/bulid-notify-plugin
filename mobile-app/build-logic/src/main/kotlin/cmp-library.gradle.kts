plugins {
    id("kmp-library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

val catalog = the<VersionCatalogsExtension>().named("libs")

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(catalog.findLibrary("jetbrains-compose-runtime").get())
            implementation(catalog.findLibrary("jetbrains-compose-ui").get())
            implementation(catalog.findLibrary("jetbrains-compose-foundation").get())
        }
    }
}
