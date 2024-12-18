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
import com.google.protobuf.Message
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.exists
import net.harawata.appdirs.AppDirsFactory

/**
 * Stores data on disk in a sequence of bytes.
 *
 * @param T The type of the stored message.
 *
 * @param location The location of a file on disk.
 */
internal class FileStorage<T : Message>(location: FileLocation) {
    /**
     * A file on disk, serving as a repository.
     */
    private val file: File

    init {
        // If the file or its parent directories do not exist, they are created.
        val parent = Path(location.dir)
        if (!parent.exists()) {
            parent.createDirectories()
        }
        val child = parent.resolve(location.name)
        if (!child.exists()) {
            child.createFile()
        }
        file = child.toFile()
    }

    /**
     * Returns the data from the storage file if it contains content;
     * otherwise, returns the [default].
     *
     * @param parser Deserializes a byte sequence into a message.
     * @param default Calculates the default value when no data is found in the file.
     */
    internal fun loadOrDefault(parser: (ByteArray) -> T, default: () -> T): T {
        FileInputStream(file).use { stream ->
            val bytes = stream.readAllBytes()
            return if (bytes.isNotEmpty()) {
                parser(bytes)
            } else {
                default()
            }
        }
    }

    /**
     * Writes [message] to the storage file.
     *
     * @param message The message that is serialized and saved to a file.
     */
    internal fun save(message: T) {
        FileOutputStream(file).use { stream ->
            message.toByteString().writeTo(stream)
        }
    }
}

/**
 * A location of a file on disk.
 *
 * @property dir The absolute path to the directory where the file is located.
 * @property name The name of the file.
 */
internal class FileLocation private constructor(
    internal val dir: String,
    internal val name: String
) {
    internal companion object {
        /**
         * The path to application data within the user’s home directory.
         */
        private val appDirPath = AppDirPath.withoutVersion()

        /**
         * Creates a location of the file within the application's folder
         * in the user data directory.
         */
        internal fun inAppDir(fileName: String): FileLocation =
            FileLocation(appDirPath, fileName)
    }
}

/**
 * Deletes all files in the application's folder
 * within the user data directory.
 *
 * For testing purposes only.
 */
@VisibleForTesting
public fun clearAppDir() {
    val path = AppDirPath.withoutVersion()
    File(path).listFiles()?.forEach { file ->
        if (file.isFile) {
            file.delete()
        } else {
            file.deleteRecursively()
        }
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
