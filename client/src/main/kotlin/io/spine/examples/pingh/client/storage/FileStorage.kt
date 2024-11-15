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

import com.google.common.annotations.VisibleForTesting
import com.google.protobuf.Message
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.exists

/**
 * Stores data on disk in a sequence of bytes.
 */
internal object FileStorage {
    /**
     * Returns the data from the storage file if it contains content;
     * otherwise, returns the `default`.
     *
     * @param T The type of message being loaded.
     *
     * @param from The location of the file from which the data is read.
     * @param parser Deserializes a byte sequence into a message.
     * @param default Calculates the default value to use when no data is found in the file.
     */
    internal inline fun <reified T : Message> loadOrDefault(
        from: FileLocation,
        parser: (ByteArray) -> T,
        default: () -> T
    ): T {
        FileInputStream(storage(from)).use { file ->
            val bytes = file.readAllBytes()
            return if (bytes.isNotEmpty()) {
                parser(bytes)
            } else {
                default()
            }
        }
    }

    /**
     * Writes `message` to the storage file.
     *
     * @param T The type of message being saved.
     *
     * @param to The location of the file to which the data is written.
     * @param message The message that is serialized and saved to a file.
     */
    internal fun <T : Message> save(to: FileLocation, message: T) {
        FileOutputStream(storage(to)).use { file ->
            message.toByteString().writeTo(file)
        }
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
