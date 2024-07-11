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

package io.spine.internal.gradle.publishing

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.findByType

/**
 * Configures the [PublishToMavenLocal] extension.
 *
 * This extension sets up publishing of artifacts to Maven Local repository.
 *
 * The extension can be configured only for multi-module projects. The extension should be opened
 * in the root project's build file. The published modules are specified explicitly by their names
 * or paths:
 *
 * ```
 * publishToMavenLocal {
 *     modules = setOf(
 *         "subprojectA",
 *         "subprojectB"
 *     )
 * }
 * ```
 *
 * In Gradle, in order to publish something somewhere one should create a publication.
 * In each of published modules, the extension will create a [publication][PublicationHandler]
 * named `"mavenJava"`. All artifacts, published by this extension belong to this publication.
 *
 * @see [PublicationHandler]
 */
public fun Project.publishToMavenLocal(block: PublishToMavenLocal.() -> Unit) {
    apply<MavenPublishPlugin>()
    val name = PublishToMavenLocal::class.java.simpleName
    val extension = with(extensions) {
        findByType<PublishToMavenLocal>() ?: create(name, project)
    }
    extension.run {
        block()
        configured()
    }
}

/**
 * A Gradle extension for setting up publishing of modules to Maven Local repository
 * using `maven-publish` plugin.
 *
 * @param project a project in which extension is opened.
 * @see [publishToMavenLocal]
 */
public open class PublishToMavenLocal(
    private val project: Project
) {

    /**
     * Sets of modules to be published.
     *
     * Both of module's name or path can be used.
     *
     * This property must be specified, and the set must be non-empty.
     * Otherwise, [configuring][configured] such an extension throws a [GradleException].
     */
    public lateinit var modules: Set<String>

    /**
     * Notifies the extension that its configuration is completed.
     *
     * Adds the `maven-publish` plugin to each published module and
     * configures their publication properties.
     */
    internal fun configured() {
        projectToPublish()
            .ifEmpty { throw GradleException("No modules were found to publish.") }
            .forEach { project ->
                project.setUpPublishing()
            }
    }

    /**
     * Maps the names of published modules to `Project` instances.
     *
     * @see [modules]
     */
    private fun projectToPublish(): Set<Project> =
        modules
            .map { name -> project.project(name) }
            .toSet()

    /**
     * Sets up the `maven-publish` plugin to this project and configures it.
     *
     * Creates the [PublicationHandler] for the project and
     * schedules to apply it on [Project.afterEvaluate].
     *
     * The `afterEvaluate` is used to have access to information from already compiled modules.
     * This allows obtaining information about the module's group, its versions, and so on.
     */
    private fun Project.setUpPublishing() {
        val handler = PublicationHandler(project)
        afterEvaluate {
            handler.apply()
        }
    }
}
