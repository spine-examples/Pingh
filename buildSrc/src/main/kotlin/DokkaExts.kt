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

import io.spine.internal.BuildSettings
import io.spine.internal.dependency.Dokka
import org.gradle.kotlin.dsl.DependencyHandlerScope
import java.io.File
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.gradle.DokkaTask

/**
 * To exclude pieces of code annotated with `@Internal` from the documentation
 * a custom plugin is added to the Dokka's classpath.
 *
 * @see <a href="https://github.com/SpineEventEngine/dokka-tools/tree/master/dokka-extensions">
 *     Custom Dokka Plugins</a>
 */
public fun DependencyHandlerScope.useDokkaWithSpineExtensions() {
    dokkaPlugin(Dokka.SpineExtensions.lib)
}

private fun DependencyHandler.dokkaPlugin(dependencyNotation: Any): Dependency? =
    add("dokkaPlugin", dependencyNotation)

/**
 * Configures this [DokkaTask] to accept Kotlin files.
 */
public fun DokkaTask.configureForKotlin() {
    dokkaSourceSets.configureEach {
        /**
         * Configures links to the external Java documentation.
         */
        jdkVersion.set(BuildSettings.javaVersion.asInt())

        skipEmptyPackages.set(true)

        documentedVisibilities.set(
            setOf(
                DokkaConfiguration.Visibility.PUBLIC,
                DokkaConfiguration.Visibility.PROTECTED
            )
        )
    }

    outputDirectory.set(project.dokkaOutput("kotlin"))
}

private fun Project.dokkaOutput(language: String): File =
    buildDir.resolve("docs/dokka${language.capitalize()}")
