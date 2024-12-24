/*
 * Copyright 2024, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import io.spine.internal.dependency.Coil
import io.spine.internal.dependency.Compose
import io.spine.internal.dependency.Guava
import io.spine.internal.dependency.Pingh
import io.spine.internal.gradle.AppVersion
import io.spine.internal.gradle.allowBackgroundExecution
import io.spine.internal.gradle.extractSemanticVersion
import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")

    // Adds dependencies for Dokka and configures it.
    id("dokka-configuration")

    // Adds and configures the Detekt for analysis code.
    id("detekt-code-analysis")

    // Adds dependencies for testing and configure test-running tasks.
    id("tests-configuration")
}

/**
 * The path to the parent project.
 *
 * This project is nested standalone. Its parent contains project's version.
 */
private val parentRootDir = rootDir.parent

apply(from = "$parentRootDir/version.gradle.kts")

/**
 * The last version of the Pingh project.
 */
private val pinghVersion = AppVersion(extra["pinghVersion"] as String)

group = "io.spine.example.pingh"
version = pinghVersion.value

repositories {
    mavenLocal()
    mavenCentral()
    google()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven("https://spine.mycloudrepo.io/public/repositories/releases")
}

kotlin {
    explicitApi()
}

configurations.all {
    resolutionStrategy {
        force(Guava.lib)
    }
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.components.resources)
    implementation(Compose.Material3.lib)
    implementation(Guava.lib)
    implementation(Coil.compose)
    implementation(Coil.network)
    implementation(Pingh.client)

    testImplementation(Pingh.testutilClient)
    testImplementation(kotlin("test"))
    @OptIn(ExperimentalComposeLibrary::class)
    testImplementation(compose.uiTest)
}

compose.resources {
    customDirectory(
        sourceSetName = "main",
        directoryProvider = provider { layout.projectDirectory.dir("src/main/composeResources") }
    )
}

compose.desktop {
    application {
        mainClass = "io.spine.examples.pingh.desktop.MainKt"
        nativeDistributions {
            packageName = "Pingh"
            targetFormats(TargetFormat.Dmg)
            packageVersion = pinghVersion.extractSemanticVersion().value
            macOS {
                iconFile = iconForMacOs()
                infoPlist {
                    allowBackgroundExecution()
                }
            }
        }
    }
}

/**
 * Returns a `.icns` file containing the Pingh icon.
 */
private fun iconForMacOs() = File("distribution-resources/icons/pingh.icns")
