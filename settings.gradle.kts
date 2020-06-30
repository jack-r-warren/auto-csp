rootProject.name = "auto-csp"
include("core", "directives")

enableFeaturePreview("GRADLE_METADATA")

pluginManagement {
    val kotlinVersion: String by settings
    val jgitverVersion: String by settings
    val shadowVersion: String by settings

    plugins {
        kotlin("jvm") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion
        id("fr.brouillard.oss.gradle.jgitver") version jgitverVersion
        id("com.github.johnrengelman.shadow") version shadowVersion
    }

    repositories {
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
        mavenCentral()
        gradlePluginPortal()
    }
}


