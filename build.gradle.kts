import com.github.jengelman.gradle.plugins.shadow.ShadowPlugin
import fr.brouillard.oss.gradle.plugins.JGitverPlugin
import fr.brouillard.oss.gradle.plugins.JGitverPluginExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformJvmPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import org.jetbrains.kotlinx.serialization.gradle.SerializationGradleSubplugin

plugins {
    kotlin("jvm") apply false
    kotlin("plugin.serialization") apply false
    id("fr.brouillard.oss.gradle.jgitver") apply false
    id("com.github.johnrengelman.shadow") apply false
}

group = "info.jackwarren"

val ktorVersion: String by project
val kotlinxSerializationVersion: String by project
val cliktVersion: String by project
val slf4jVersion: String by project
val seleniumVersion: String by project

val kotlinConfiguration: KotlinJvmProjectExtension.() -> Unit = {
    sourceSets.configureEach {
        languageSettings.apply {
            progressiveMode = true
            enableLanguageFeature("InlineClasses")
            useExperimentalAnnotation("kotlin.ExperimentalStdlibApi")
            useExperimentalAnnotation("io.ktor.directives.util.KtorExperimentalAPI")
            useExperimentalAnnotation("kotlinx.coroutines.ObsoleteCoroutinesApi")
            useExperimentalAnnotation("kotlin.time.ExperimentalTime")
        }
        dependencies {
            implementation(kotlin("stdlib-jdk8"))
        }
    }
    target.compilations.configureEach {
        kotlinOptions {
            jvmTarget = "13"
        }
    }
}

allprojects {
    apply<IdeaPlugin>()
    apply<JGitverPlugin>()

    configure<JGitverPluginExtension> {
        useDirty = true
    }

    repositories {
        mavenCentral()
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
        maven("https://kotlin.bintray.com/kotlinx")
        maven("https://kotlin.bintray.com/ktor")
    }
}

project(":core") {
    apply<SerializationGradleSubplugin>()
    apply<KotlinPluginWrapper>()

    configure(kotlinConfiguration)
    configure<KotlinJvmProjectExtension> {
        sourceSets.configureEach {
            dependencies {
                implementation(project(":directives"))
                implementation("io.ktor:ktor-server-jetty:$ktorVersion")
                implementation("io.ktor:ktor-client-jetty:$ktorVersion")
                implementation("io.ktor:ktor-serialization:$ktorVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$kotlinxSerializationVersion")
                implementation("com.github.ajalt:clikt:$cliktVersion")
                implementation("org.slf4j:slf4j-jdk14:$slf4jVersion")
                implementation("org.seleniumhq.selenium:selenium-java:$seleniumVersion")
            }
        }
    }
}

project(":directives") {
    apply<SerializationGradleSubplugin>()
    apply<KotlinPluginWrapper>()

    configure(kotlinConfiguration)
    configure<KotlinJvmProjectExtension> {
        sourceSets.configureEach {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$kotlinxSerializationVersion")
            }
        }
    }
}

listOf<Pair<String, ApplicationPluginConvention.() -> Unit>>(
    ":core" to { mainClassName = "core.MainKt" }
).forEach { (projectName, applicationConfiguration) ->
    project(projectName) {
        apply<ApplicationPlugin>()
        apply<ShadowPlugin>()

        configure(applicationConfiguration)
    }
}