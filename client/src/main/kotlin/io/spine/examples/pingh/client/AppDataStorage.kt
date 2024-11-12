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
import net.harawata.appdirs.AppDirsFactory

private val gson = Gson()

internal object AppDataStorage {
    private const val storageName = ".storage.json"
    private val appDirPath = AppDirPath.withoutVersion()

    private lateinit var state: AppState

    init {
        load()
    }

    /**
     * Returns a file for storing the application state.
     *
     * If the file or its parent directories do not exist, they are created.
     */
    private fun storage(): File {
        val parent = Path(appDirPath)
        if (!parent.exists()) {
            parent.createDirectories()
        }
        val child = parent.resolve(storageName)
        if (!child.exists()) {
            child.createFile()
        }
        return child.toFile()
    }

    private fun load() {
        val content = storage().readText()
        if (content.isNotBlank()) {
            state = gson.fromJson(content, AppState::class.java)
        } else {
            state = AppState(AppSettings(false, SnoozeTime.TWO_HOURS))
            save()
        }
    }

    internal val data: AppState
        get() = state

    internal fun save() {
        val json = gson.toJson(state)
        storage().writeText(json)
    }
}

internal data class AppState(
    var settings: AppSettings
)

internal data class AppSettings(
    var enabledDndMode: Boolean,
    var snoozeTime: SnoozeTime
)

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
