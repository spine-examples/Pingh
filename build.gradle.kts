/*
 * Copyright 2024, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

/**
 * Gradle configuration for the whole project.
 */
allprojects {
    /*
    * Import the `version.gradle.kts` file and set the version and group for each module.
    */
    apply(from = "$rootDir/version.gradle.kts")
    version = extra["pinghVersion"]!!
    group = "io.spine.examples"

    apply<IdeaPlugin>()
}

/**
 * The configuration is divided in multiple script plugins located in `buildSrc/src/main/kotlin`.
 * Each of these plugins contains a more detailed description in their source file.
 */
subprojects {

    apply<JavaPlugin>()

    /*
     * Configure repositories.
     */
    apply<RepositoriesConfigurationPlugin>()

    /*
     * Adds dependencies for Dokka and configures it.
     */
    apply<DokkaConfigurationPlugin>()

    /*
     * Adds dependencies for testing and configure test-running tasks.
     */
    apply<TestsConfigurationPlugin>()
}
