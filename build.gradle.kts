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

import io.spine.internal.gradle.RunGradleBuild
import io.spine.internal.gradle.publishing.publishToMavenLocal

plugins {
    java
}

/**
 * Gradle configuration for the whole project.
 */
allprojects {
    // Import the `version.gradle.kts` file and set the version and group for each module.
    apply(from = "$rootDir/version.gradle.kts")
    version = extra["pinghVersion"]!!
    group = "io.spine.examples.pingh"

    apply<IdeaPlugin>()
}

/**
 * The configuration is divided in multiple script plugins located in `buildSrc/src/main/kotlin`.
 * Each of these plugins contains a more detailed description in their source file.
 */
subprojects {

    apply<JavaPlugin>()

    // Adds Kotlin plugin and configures it.
    apply<KotlinConfigurationPlugin>()

    // Configure repositories.
    apply<RepositoriesConfigurationPlugin>()

    // Adds dependencies for Dokka and configures it.
    apply<DokkaConfigurationPlugin>()

    // Adds and configures the Detekt Plugin for analysis code.
    apply<DetektCodeAnalysisPlugin>()

    // Adds dependencies for testing and configure test-running tasks.
    apply<TestsConfigurationPlugin>()
}

/**
 * The set of names of modules that required for building the `desktop` standalone project.
 */
val modulesRequiredForDesktop = setOf(
    "time",
    "github",
    "sessions",
    "mentions",
    "server",
    "client"
)

/**
 * Publishes modules required for building the `desktop` standalone project
 * to the Maven Local repository.
 */
publishToMavenLocal {
    modules = modulesRequiredForDesktop
}

/**
 * The task that builds the standalone Gradle project in the `desktop` directory.
 *
 * This task depends on publishing to the Local Maven repository the modules
 * required for the `desktop` project.
 */
val buildDesktopClient = tasks.register<RunGradleBuild>("buildDesktopClient") {
    val task = this
    directoryPath = "$rootDir/desktop"
    modulesRequiredForDesktop.forEach { name ->
        task.dependsOn(":$name:publishToMavenLocal")
    }
}

/**
 * Adds the publishing of modules required for the `desktop` project
 * to the Local Maven repository as part of the build process.
 */
modulesRequiredForDesktop.forEach { name ->
    tasks.build {
        dependsOn(":$name:publishToMavenLocal")
    }
}
