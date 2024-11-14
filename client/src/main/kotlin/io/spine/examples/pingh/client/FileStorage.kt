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

package io.spine.examples.pingh.client

import com.google.common.annotations.VisibleForTesting
import com.google.gson.Gson
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.exists
import net.harawata.appdirs.AppDirsFactory

/**
 * A repository that stores data in JSON format on a file on disk.
 */
internal object FileStorage {
    private val gson = Gson()

    /**
     * Returns the data from the storage file if it contains content;
     * otherwise, returns the `default`.
     *
     * @param T The type of class being loaded.
     *
     * @param from The location of the file from which the data is read.
     * @param default Calculates the default value to use when no data is found in the file.
     */
    internal inline fun <reified T> loadOrDefault(from: FileLocation, default: () -> T): T {
        val content = storage(from).readText()
        return if (content.isNotBlank()) {
            gson.fromJson(content, T::class.java)
        } else {
            default()
        }
    }

    /**
     * Writes `data` to the storage file.
     *
     * @param T The type of class being saved.
     *
     * @param to The location of the file to which the data is written.
     * @param data The object that is converted to JSON and written to a file.
     */
    internal fun <T> save(to: FileLocation, data: T) {
        val json = gson.toJson(data)
        storage(to).writeText(json)
    }

    /**
     * Deletes a file by its location.
     *
     * If the file does not exist, does nothing.
     *
     * @param file The location of the file to be deleted.
     */
    internal fun delete(file: FileLocation) {
        storage(file).apply {
            if (exists()) {
                delete()
            }
        }
    }

    /**
     * Returns a file for storing the application state.
     *
     * If the file or its parent directories do not exist, they are created.
     */
    private fun storage(file: FileLocation): File {
        val parent = Path(file.dir)
        if (!parent.exists()) {
            parent.createDirectories()
        }
        val child = parent.resolve(file.name)
        if (!child.exists()) {
            child.createFile()
        }
        return child.toFile()
    }
}

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
        internal val Session = FileLocation(appDirPath, ".session.json")

        /**
         * The location of the application settings file.
         */
        internal val Settings = FileLocation(appDirPath, ".settings.json")
    }
}

/**
 * Deletes all files containing application state information
 * in the user data directory.
 *
 * For testing purposes only.
 */
@VisibleForTesting
public fun clearFileStorage() {
    FileStorage.delete(FileLocation.Session)
    FileStorage.delete(FileLocation.Settings)
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
