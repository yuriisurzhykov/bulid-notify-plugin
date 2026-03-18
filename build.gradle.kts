import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.date
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(libs.plugins.jetbrains.kotlin.kover)
    alias(libs.plugins.jetbrains.platform)
    alias(libs.plugins.jetbrains.qodana)
    alias(libs.plugins.jetbrains.changelog)
}

group = providers.gradleProperty("plugin.groupId")
version = providers.gradleProperty("plugin.version").get()

repositories {
    mavenCentral()
    intellijPlatform {
        marketplace()
        defaultRepositories()
        androidStudioInstallers()
    }
}

// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    intellijPlatform {
        androidStudio(providers.gradleProperty("platformVersion").get())
        testFramework(TestFrameworkType.Platform)
        pluginVerifier()
        zipSigner()

        composeUI()

        bundledPlugins(
            "com.intellij.gradle",
            "org.jetbrains.kotlin",
        )
    }

    implementation(libs.java.websockets)
    implementation(libs.java.mdns)
    implementation(libs.kotlin.serialization.json)

    compileOnly(libs.kotlin.serialization.core)
    compileOnly(libs.kotlin.coroutines.core)

    testImplementation(libs.test.junit)
    testImplementation(libs.test.mockk)
    testImplementation(libs.test.opentest4j)
    testImplementation(libs.kotlin.coroutines.test)
}

intellijPlatform {
    pluginConfiguration {
        name = providers.gradleProperty("plugin.name")
        version = providers.gradleProperty("plugin.version")
        description = """
            <p>
            Sensds Gradle build results to your Android phone over local Wi-Fi.
            Multiple devices supported simultaneously. </br>
            Zero configuration - discovered automatically via mDNS.
            </p>
            <ul>
            <li>Real-time build status: SUCCESS / FAILED / CANCELLED</li>
            <li>Detailed Kotlin and Java compiler errors and warnings</li>
            <li>Heartbeat keeps mobile connected alive between builds.</li>
            </ul>
        """.trimIndent()

        ideaVersion {
            sinceBuild = providers.gradleProperty("plugin.sinceBuild")
            untilBuild = providers.gradleProperty("plugin.untilBuild")
        }

        val changelog = project.changelog
        changeNotes = providers.gradleProperty("plugin.version").map { version ->
            with(changelog) {
                renderItem(
                    (getOrNull(version) ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    Changelog.OutputType.HTML,
                )
            }
        }
    }

    signing {
        // Fill in when publishing to JetBrains Marketplace.
        // certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        // privateKey       = providers.environmentVariable("PRIVATE_KEY")
        // password         = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        // Set PUBLISH_TOKEN in your environment before running publishPlugin
        token = providers.environmentVariable("PUBLISHING_TOKEN")
    }

    pluginVerification {
        ides {

        }
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}


changelog {
    version = providers.gradleProperty("plugin.version").get()
    path = file("CHANGELOG.md").canonicalPath
    header = provider { "[${version.get()}] - ${date()}" }
    headerParserRegex = """(\d+\.\d+\.\d+)""".toRegex()
    itemPrefix = "-"
    keepUnreleasedSection = true
    unreleasedTerm = "[Unreleased]"
    groups = listOf("Added", "Changed", "Deprecated", "Removed", "Fixed", "Security")
    combinePreReleases = true
}

tasks {
    test {
        useJUnitPlatform()
    }

    // Ensure changelog is initialized before patching plugin.xml.
    patchPluginXml {

    }

    // When bumping pluginVersion in gradle.properties,
    // run `./gradlew patchChangelog` to move [Unreleased] → [x.y.z].
    patchChangelog {
        // No extra config needed — picks up version from changelog extension.
    }
}