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
 *
 * @param T The type of the stored message.
 */
internal class FileStorage<T : Message>(location: FileLocation) {
    /**
     * A file on disk, serving as a repository.
     */
    private val file: File

    /**
     * If the file or its parent directories do not exist, they are created.
     */
    init {
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
     * otherwise, returns the `default`.
     *
     * @param parser Deserializes a byte sequence into a message.
     * @param default The default value returned when no data is found in the file.
     */
    internal fun loadOrDefault(parser: (ByteArray) -> T, default: T): T {
        FileInputStream(file).use { stream ->
            val bytes = stream.readAllBytes()
            return if (bytes.isNotEmpty()) {
                parser(bytes)
            } else {
                default
            }
        }
    }

    /**
     * Writes `message` to the storage file.
     *
     * @param message The message that is serialized and saved to a file.
     */
    internal fun save(message: T) {
        FileOutputStream(file).use { stream ->
            message.toByteString().writeTo(stream)
        }
    }

    /**
     * Clears all data inside the storage file.
     */
    internal fun clear() {
        FileOutputStream(file).close()
    }
}
