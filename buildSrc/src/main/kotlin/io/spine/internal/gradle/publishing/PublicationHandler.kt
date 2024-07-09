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

import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.create

/**
 * A publication for a Java project to the Maven Local repository.
 *
 * In Gradle, to publish something, one should create a publication.
 * A publication has a name and consists of one or more artifacts plus information about
 * those artifacts – the metadata.
 *
 * An instance of this class represents [MavenPublication] named `"mavenJava"`. It is generally
 * accepted that a publication with this name contains a Java project published to
 * the Maven Local repository.
 *
 * @param project a published `Project`.
 * @see <a href="https://docs.gradle.org/current/userguide/publishing_maven.html#publishing_maven:publications">
 *       Maven Publish Plugin | Publications</a>
 */
public class PublicationHandler(
    private val project: Project
) {

    private companion object {
        /**
         * The name of the Maven Publishing Gradle plugin.
         */
        private const val mavenPublish = "maven-publish"
    }

    /**
     * Creates a new `"mavenJava"` [MavenPublication] in this given project.
     */
    public fun apply() {
        with(project) {
            apply(plugin = mavenPublish)
            pluginManager.withPlugin(mavenPublish) {
                val publications = project.publications
                publications.create<MavenPublication>("mavenJava") {
                    copyAttributes()
                    specifyArtifacts()
                }
            }
        }
    }

    /**
     * Copies the attributes of the Gradle [Project] to this `MavenPublication`.
     *
     * The following project attributes are copied:
     * - [group][Project.getGroup];
     * - [artifact][Project.getName];
     * - [version][Project.getVersion].
     */
    private fun MavenPublication.copyAttributes() {
        groupId = project.group.toString()
        artifactId = project.name
        version = project.version.toString()
    }

    /**
     * Specifies which artifacts this `MavenPublication` contains.
     *
     * This Maven publication contains:
     *
     *  1. Jar archives: compilation output, sources, and Kotlin Dokka docs.
     *  2. Maven metadata file that has ".pom" extension.
     *  3. Gradle's metadata file that has ".module" extension.
     *
     * @see <a href="https://docs.gradle.org/current/userguide/publishing_gradle_module_metadata.html">
     *      Understanding Gradle Module Metadata</a>
     */
    private fun MavenPublication.specifyArtifacts() {
        val javaComponent = project.components.findByName("java")
        javaComponent?.let { from(it) }
        project.addSourcesJar()
        artifact(project.dokkaKotlinJar())
    }
}
