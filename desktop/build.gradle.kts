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

plugins {
    kotlin("jvm").version("1.9.20")
    id("org.jetbrains.compose").version("1.6.11")
    id("io.gitlab.arturbosch.detekt")
}

/**
 * The path to the parent project.
 *
 * This project is nested standalone.
 * Its parent contains project's version and Detekt configs.
 */
private val parentRootDir = rootDir.parent

apply(from = "$parentRootDir/version.gradle.kts")

/**
 * The last version of the Pingh project.
 */
private val pinghVersion = extra["pinghVersion"]!!

private val jvmVersion = 11

group = "io.spine.example.pingh"
version = pinghVersion

repositories {
    mavenLocal()
    mavenCentral()
    google()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven("https://spine.mycloudrepo.io/public/repositories/releases")
}

kotlin {
    jvmToolchain(jvmVersion)
    explicitApi()
}

configurations {
    all {
        resolutionStrategy {
            force("com.google.guava:guava:31.1-jre")
        }
    }
}

dependencies {
    implementation("androidx.compose.runtime:runtime:1.6.0")
    implementation(compose.desktop.currentOs)
    implementation("io.spine.examples.pingh:client:$pinghVersion")
}
