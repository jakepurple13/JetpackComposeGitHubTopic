@file:OptIn(ExperimentalComposeLibrary::class)

import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10"
    id("org.jetbrains.compose") version "1.1.0"
}

group = "me.jrein"
version = "1.0"

val exposedVersion = "0.37.3"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.materialIconsExtended)
    implementation(compose.material3)
    implementation(compose.desktop.components.splitPane)
    implementation("com.github.tsohr:json:0.0.2")
    implementation("com.mikepenz:multiplatform-markdown-renderer-jvm:0.4.0")
    implementation("org.ocpsoft.prettytime:prettytime:5.0.2.Final")
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("com.h2database:h2:2.1.210")
    implementation("org.xerial:sqlite-jdbc:3.36.0.2")
    implementation("com.google.code.gson:gson:2.9.0")
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "11"
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "GitHub Topics"
            packageVersion = "1.0.0"
            modules("java.instrument", "java.prefs", "java.sql", "jdk.unsupported")
            appResourcesRootDir.set(project.layout.projectDirectory.dir("src/main/resources"))
            macOS {
                iconFile.set(project.file("src/main/resources/logo.icns"))
            }
            windows {
                iconFile.set(project.file("src/main/resources/logo.ico"))
            }
            linux {
                iconFile.set(project.file("src/main/resources/logo.png"))
            }
        }
    }
}