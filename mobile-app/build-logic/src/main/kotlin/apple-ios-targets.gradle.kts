import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Convention plugin that adds Apple/iOS targets to any KMP module.
 *
 * macOS  → registers iosX64 + iosArm64 + iosSimulatorArm64, configures the XCFramework,
 *           and wires up KSP for all three targets.
 * Windows / Linux → this file is a complete no-op. Zero Apple tasks are registered.
 */
if (OperatingSystem.current().isMacOsX) {

    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        extensions.configure<KotlinMultiplatformExtension> {
            listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { target ->
                target.binaries.framework {
                    baseName = project.name
                    isStatic = true
                }
            }
        }
    }

    plugins.withId("com.google.devtools.ksp") {
        // Type-safe `libs` accessor is not generated in precompiled script plugins.
        // Access the consuming project's version catalog explicitly instead.
        val compiler = the<VersionCatalogsExtension>()
            .named("libs")
            .findLibrary("kotlin-inject-compiler")
            .get()
        dependencies {
            add("kspIosX64", compiler)
            add("kspIosArm64", compiler)
            add("kspIosSimulatorArm64", compiler)
        }
    }
}
