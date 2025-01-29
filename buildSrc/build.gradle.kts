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
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
    mavenLocal()
}

kotlin {
    explicitApiWarning()
}

/**
 * The version of Kotlin used for compile `buildSrc` module.
 *
 * Kotlin 1.4.20 is the highest possible version for Gradle 6.9.4 configuration.
 *
 * This version can be different with Kotlin version in other modules.
 */
val kotlinVersion = "1.4.20"

/**
 * The version of the Detekt Gradle Plugin.
 *
 * @see <a href="https://github.com/detekt/detekt/releases">Detekt Releases</a>
 */
val detektVersion = "1.15.0"

/**
 * The version of Dokka Gradle Plugins.
 *
 * @see <a href="https://github.com/Kotlin/dokka/releases">Dokka Releases</a>
 */
val dokkaVersion = "1.9.20"

/**
 * The ShadowJar Gradle plugin.
 *
 * The plugin is now published under the ID `com.gradleup.shadow`
 * and requires Gradle 8.x or higher.
 * Since the project uses Gradle 6.x, the old plugin ID remains in use.
 *
 * The plugin will be updated after upgrading Gradle.
 *
 * @see <a href="https://github.com/GradleUp/shadow/releases">Shadow Plugin Releases</a>
 */
object ShadowJarPlugin {
    /**
     * The 6.1.0 version is the last version compatible with Gradle 6.x.
     */
    private const val version = "6.1.0"
    private const val group = "com.github.jengelman.gradle.plugins"
    const val lib: String = "$group:shadow:$version"
}

configurations.all {
    resolutionStrategy {
        force(
            // Force Kotlin lib versions avoiding using those bundled with Gradle.
            "org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion",
            "org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion",
            "org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion"
        )
    }
}

dependencies {
    implementation(ShadowJarPlugin.lib)
    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:$detektVersion")
    implementation("org.jetbrains.dokka:dokka-base:$dokkaVersion")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:$dokkaVersion")

    // Kotlin plugin, which is used to run Dokka, and is also added to Gradle classpath,
    // from where it is used in other modules. For Dokka to work correctly above 1.4 version,
    // need to use a version of Kotlin higher than 1.4.
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.20")
}
