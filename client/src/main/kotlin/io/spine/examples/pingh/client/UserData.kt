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

import com.google.gson.Gson
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.reflect.KClass
import net.harawata.appdirs.AppDirsFactory

private val gson = Gson()

/**
 * The path to application data within the user’s home directory.
 */
private val appDirPath = AppDirPath.withoutVersion()

/**
 * User data stored on the device.
 *
 * Data is initially loaded from [storage][FileStorage].
 * If the `storage` is empty, the `default` value is used.
 *
 * To preserve data between restarts, the current [data] must be [saved][save] manually.
 *
 * @param T The type of data stored.
 *
 * @param storageFileName The name of the file where user data is stored.
 * @param type The class of the stored data type.
 * @param default The default value to use if the repository is empty.
 */
@Suppress("UnnecessaryAbstractClass" /* Avoids creating instances; only for inheritance. */)
internal abstract class UserData<T : Any>(
    storageFileName: String,
    default: () -> T,
    type: KClass<T>
) {
    /**
     * A repository that allows reading from and writing user data to the disk.
     */
    private val storage = FileStorage(storageFileName, appDirPath, type)

    /**
     * A current user data.
     */
    internal val data: T = storage.loadOr(default)

    /**
     * Saves the current user [data] to a file on disk.
     */
    internal fun save() {
        storage.save(data)
    }
}

/**
 * A repository that stores data in JSON format on a file on disk.
 */
private class FileStorage<T : Any>(
    private val fileName: String,
    private val dir: String,
    private val type: KClass<T>
) {
    /**
     * Returns a file for storing the application state.
     *
     * If the file or its parent directories do not exist, they are created.
     */
    fun storage(): File {
        val parent = Path(dir)
        if (!parent.exists()) {
            parent.createDirectories()
        }
        val child = parent.resolve(fileName)
        if (!child.exists()) {
            child.createFile()
        }
        return child.toFile()
    }

    /**
     * Returns the data from the storage file if it contains content;
     * otherwise, returns the `default`.
     */
    fun loadOr(default: () -> T): T {
        val content = storage().readText()
        return if (content.isNotBlank()) {
            gson.fromJson(content, type.java)
        } else {
            default()
        }
    }

    /**
     * Writes `data` to the storage file.
     */
    fun save(data: T) {
        val json = gson.toJson(data)
        storage().writeText(json)
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
