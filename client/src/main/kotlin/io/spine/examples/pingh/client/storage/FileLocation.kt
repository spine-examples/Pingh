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

package io.spine.examples.pingh.client.storage

import net.harawata.appdirs.AppDirsFactory

/**
 * A location of a file on disk.
 *
 * @property dir The absolute path to the directory where the file is located.
 * @property name The name of the file.
 */
internal data class FileLocation(
    internal val dir: String,
    internal val name: String
) {
    internal companion object {
        /**
         * The path to application data within the user’s home directory.
         */
        private val appDirPath = AppDirPath.withoutVersion()

        /**
         * The location of the file with user session data.
         */
        internal val Session = FileLocation(appDirPath, ".session")

        /**
         * The location of the application settings file.
         */
        internal val Settings = FileLocation(appDirPath, ".settings")
    }
}

/**
 * Provides the path to platform-specific application data
 * within the user’s home directory.
 */
private object AppDirPath {
    private const val author = "spine-examples"
    private const val name = "Pingh"
    private const val version = "1.0.0"

    /**
     * Returns the path to the application data,
     * without including the current application version.
     */
    fun withoutVersion(): String {
        val versionedPath = AppDirsFactory.getInstance()
            .getUserDataDir(name, version, author)
        return versionedPath.substring(0, versionedPath.length - version.length - 1)
    }
}
