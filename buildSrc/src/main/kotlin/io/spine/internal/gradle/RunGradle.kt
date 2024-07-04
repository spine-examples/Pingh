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

package io.spine.internal.gradle

import java.util.concurrent.TimeUnit
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.os.OperatingSystem

/**
 * The Gradle task which runs another Gradle build.
 */
public open class RunGradle : DefaultTask() {

    private companion object {
        /**
         * Default Gradle build timeout.
         */
        private const val buildTimeoutMinutes: Long = 10
    }

    /**
     * Path to the directory which contains a Gradle wrapper script.
     */
    @Internal
    public lateinit var directoryPath: String

    /**
     * The names of the task to be passed to the Gradle wrapper script.
     */
    private lateinit var taskNames: List<String>

    /**
     * Sets the names of the task to be passed to the Gradle wrapper script.
     */
    public fun tasks(vararg tasks: String) {
        taskNames = tasks.toList()
    }

    /**
     * Launches Gradle wrapper under a given [directoryPath] with
     * the specified [taskNames] names.
     */
    @TaskAction
    private fun execute() {
        val command = buildCommand()
        val process = startProcess(command)
        val completed = process.waitFor(buildTimeoutMinutes, TimeUnit.MINUTES)
        val exitCode = process.exitValue()
        if (!completed || exitCode != 0) {
            throw GradleException("Child build process failed. Exit code: $exitCode.")
        }
    }

    /**
     * Collects a list of commands that are used to create a new [Process].
     */
    private fun buildCommand(): List<String> {
        val script = selectScript()
        return listOf(
            "$directoryPath/$script",
            *taskNames.toTypedArray(),
            "--stacktrace"
        )
    }

    /**
     * Selects which script to use to build Gradle project depending on the operating system.
     */
    private fun selectScript(): String {
        val runOnWindows = OperatingSystem.current().isWindows
        return if (runOnWindows) "gradlew.bat" else "gradlew"
    }

    /**
     * Creates and starts a new operating system process.
     */
    private fun startProcess(command: List<String>): Process =
        ProcessBuilder()
            .command(command)
            .directory(project.file(directoryPath))
            .start()
}
