/*
 * Copyright 2025, TeamDev. All rights reserved.
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

package io.spine.internal.gradle

import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.file.FileTree
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.named

/**
 * Merges files matching the specified ANT path patterns from all runtime libraries
 * and saves them to the project's main resources build directory.
 *
 * Only unique lines from each file are saved in random order.
 */
public open class MergeServiceFiles : DefaultTask() {

    /**
     * Set of ANT patterns representing paths relative to JAR libraries
     * for files to be merged.
     */
    @get:Input
    public lateinit var filePatterns: Set<String>

    /**
     * All unique lines from files located at a specific path
     * within the project's runtime libraries.
     */
    private lateinit var mergedFilesContent: MutableMap<String, MutableSet<String>>

    /**
     * Merges all specified files from runtime libraries
     * and saves them to the project's main resources build directory.
     */
    @TaskAction
    private fun execute() {
        mergedFilesContent = mutableMapOf()
        mergeFilesInAllJars()
        saveMergedFiles()
    }

    /**
     * Searches the files for merging within all JAR libraries used at runtime
     * and saves their content.
     */
    private fun mergeFilesInAllJars() {
        project.sourceSets
            .main.get()
            .runtimeClasspath
            .filter { file -> file.extension == "jar" }
            .forEach { jar -> project.zipTree(jar).mergeFiles() }
    }

    /**
     * Searches the file tree for files to be merged and saves their content.
     */
    private fun FileTree.mergeFiles() {
        filePatterns.forEach { pattern ->
            this.matching { include(pattern) }
                .forEach { file ->
                    val path = file.pathBy(AntPathPattern(pattern))
                    if (mergedFilesContent[path].isNullOrEmpty()) {
                        mergedFilesContent[path] = file.readLines().toMutableSet()
                    } else {
                        mergedFilesContent[path]!!.addAll(file.readLines())
                    }
                }
        }
    }

    /**
     * Saves merged files to the project's main resources build directory.
     *
     * Required directories are created if they do not exist.
     * If a merged file exists, it is overwritten.
     */
    private fun saveMergedFiles() {
        val resourcesDir = project.layout
            .buildDirectory
            .dir("resources/main").get()
            .asFile
        if (!resourcesDir.exists()) {
            resourcesDir.mkdirs()
        }
        mergedFilesContent.forEach { (path, content) ->
            val file = resourcesDir.resolve(path)
            if (!file.parentFile.exists()) {
                file.parentFile.mkdirs()
            }
            file.deleteOnExit()
            file.writeText(content.joinToString(System.lineSeparator()))
        }
    }
}

/**
 * Returns the segment of the file's absolute path that matches the ANT path pattern.
 */
private fun File.pathBy(pattern: AntPathPattern) =
    pattern.regex().find(absolutePath)?.value ?: ""

/**
 * An ANT path pattern.
 */
private class AntPathPattern(private val pattern: String) {
    /**
     * Converts an ANT path pattern into a corresponding Regex pattern.
     */
    fun regex(): Regex =
        pattern
            .replace("/", "\\/")
            .replace(".", "\\.")
            .replace("(?<!\\*)\\*(?!\\*)".toRegex(), "[^\\/]+")
            .replace("**", ".*")
            .replace("?", "\\w")
            .toRegex()
}

/**
 * Retrieves the [sourceSets][SourceSetContainer] extension.
 */
private val org.gradle.api.Project.sourceSets: SourceSetContainer
    get() = (this as ExtensionAware).extensions.getByName("sourceSets") as SourceSetContainer

/**
 * Provides the existing [main][SourceSet] element.
 */
private val SourceSetContainer.main: NamedDomainObjectProvider<SourceSet>
    get() = named<SourceSet>("main")
