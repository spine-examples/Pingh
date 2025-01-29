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

@file:Suppress("UnusedReceiverParameter" /* Class extensions don't use class as a parameter. */)

package io.spine.examples.pingh.client

import io.spine.examples.pingh.client.FileLocation.Companion.inAppDir
import io.spine.examples.pingh.client.settings.Language
import io.spine.examples.pingh.client.settings.SnoozeTime
import io.spine.examples.pingh.client.settings.UserSettings
import io.spine.examples.pingh.client.settings.by
import io.spine.examples.pingh.client.settings.toLocale
import io.spine.examples.pingh.github.Username
import io.spine.examples.pingh.sessions.SessionId
import io.spine.validate.Validate
import java.util.Locale
import kotlin.reflect.KClass

/**
 * Manages the session with Pingh server.
 *
 * A session with the server begins when the user successfully completes
 * the login process and ends when the user logs out of the application.
 *
 * The application can have either one active session with the server or none.
 * If a user has an active session, they were the last to log in
 * and are currently using the application.
 *
 * All changes made for a user are saved to a file in the user's data directory,
 * ensuring they persist across application restarts.
 *
 * @property storage Stores user data in the application's folder
 *   within the user data directory.
 */
internal class Session(
    private val storage: UserDataStorage
) {
    /**
     * An identifier of the current session with the Pingh server.
     */
    internal val id: SessionId
        get() = storage.data.session

    /**
     * A name of the user currently using the application.
     */
    internal val username: Username
        get() = id.username

    /**
     * Whether the user currently has an active session with the server.
     *
     * If a session is active, the user is considered logged in.
     */
    internal val isActive: Boolean
        get() = storage.data.hasSession()

    /**
     * Sets a new session for the user.
     *
     * Searches for the user's local data in the registry.
     * If it does not exist, creates the user data for the session
     * and sets the [default] settings.
     */
    internal fun establish(session: SessionId) {
        storage.data = storage
            .findOrNull { it.user.equals(session.username) }
            ?.toBuilder()
            ?.setSession(session)
            ?.setDefaultSettingsIfInvalid()
            ?.vBuild()
            ?: userDataFor(session)
        storage.save()
    }

    private fun userDataFor(session: SessionId): UserData =
        UserData.newBuilder()
            .setUser(session.username)
            .setSession(session)
            .setSettings(UserSettings::class.default())
            .vBuild()

    /**
     * Clears the current user session
     * and replaces the local data with guest one.
     */
    internal fun resetToGuest() {
        storage.modifyData {
            clearSession()
        }
        storage.data = Guest.data
    }
}

/**
 * Manages the application settings configured by a user.
 *
 * Settings are user-specific. When a new user [establishes][Session.establish]
 * a session with the server, the [current] settings are replaced by those of the new user.
 *
 * All changes made by a user are saved to a file in the user's data directory,
 * ensuring persistence across application restarts.
 *
 * @property storage Stores user data in the application's folder
 *   within the user data directory.
 */
internal class Settings(
    private val storage: UserDataStorage
) {
    /**
     * An application settings applied by this user.
     */
    internal val current: UserSettings
        get() = storage.data.settings

    /**
     * Sets and saves updated application settings.
     */
    internal fun update(settings: UserSettings) {
        storage.modifyData {
            this.settings = settings
        }
        apply()
    }

    /**
     * Applies the current settings to the application.
     */
    internal fun apply() {
        Locale.setDefault(current.language.toLocale())
    }
}

/**
 * Stores user data in the application's folder
 * within the user data directory.
 *
 * Upon creation, it loads user data from storage.
 * If no data is found, [guest][Guest] data is used instead.
 *
 * Note that guest data is not saved in the repository.
 */
internal class UserDataStorage {
    /**
     * A repository for storing local data for all users.
     */
    private val storage = FileStorage<UserDataRegistry>(inAppDir(".local-data-registry"))

    /**
     * A local data for all users who have used this app on a specific device.
     */
    private var registry: UserDataRegistry =
        storage.loadOrDefault(UserDataRegistry::parseFrom) { UserDataRegistry::class.empty() }

    /**
     * A local data specific to a user, stored on the device.
     */
    var data: UserData = findOrNull { it.hasSession() }
        ?.toBuilder()
        ?.setDefaultSettingsIfInvalid()
        ?.vBuild()
        ?: Guest.data

    /**
     * Returns the first element matching the given [predicate],
     * or `null` if element was not found.
     */
    fun findOrNull(predicate: (UserData) -> Boolean): UserData? =
        registry.dataList.firstOrNull(predicate)

    /**
     * Applies the `modifier` to the builder of the current local [data]
     * and saves the resulting data.
     */
    fun modifyData(modifier: UserData.Builder.() -> Unit) {
        data = with(data.toBuilder()) {
            modifier()
            vBuild()
        }
        save()
    }

    /**
     * Updates the current [data] in the [registry] and saves it.
     */
    fun save() {
        if (data.user.equals(Guest.name)) {
            return
        }
        modifyRegistry { id ->
            if (id == -1) {
                addData(data)
            } else {
                setData(id, data)
            }
        }
    }

    /**
     * Applies the [modifier] to the builder of the [registry]
     * and saves the resulting data.
     *
     * @param modifier Updates the `registry` builder. Also provides the identifier
     *   of the current local [data] in the `registry` as a parameter.
     *   If no such data exists in the registry, it provides -1.
     */
    private fun modifyRegistry(modifier: UserDataRegistry.Builder.(id: Int) -> Unit) {
        val id = registry.dataList.indexOfFirst { it.user.equals(data.user) }
        registry = with(registry.toBuilder()) {
            modifier(id)
            vBuild()
        }
        storage.save(registry)
    }
}

/**
 * Data used for the guest account when no active sessions are available.
 */
private object Guest {
    val name: Username = Username.newBuilder()
        .setValue("Guest username")
        // Uses invalid GitHub username and avoids validation
        // so that there is no collision with real usernames.
        .buildPartial()

    val data: UserData =
        UserData.newBuilder()
            .setUser(name)
            .setSettings(UserSettings::class.default())
            .vBuild()
}

/**
 * Resets the settings to their default values if they are invalid.
 *
 * When the application is updated, settings may change.
 * To prevent errors, outdated user settings are reset if they are no longer valid.
 *
 * Use this method when loading user settings from local files.
 */
private fun UserData.Builder.setDefaultSettingsIfInvalid(): UserData.Builder {
    val violations = Validate.violationsOf(settings)
    if (violations.isNotEmpty()) {
        settings = UserSettings::class.default()
    }
    return this
}

/**
 * Creates a new `LocalDataRegistry` with an empty data list.
 */
private fun KClass<UserDataRegistry>.empty(): UserDataRegistry =
    UserDataRegistry.newBuilder().vBuild()

/**
 * Create a new `UserSettings` with default settings values.
 */
private fun KClass<UserSettings>.default(): UserSettings =
    UserSettings.newBuilder()
        .setDndEnabled(false)
        .setSnoozeTime(SnoozeTime.TWO_HOURS)
        .setLanguage(Language::class.by(Locale.getDefault()) ?: Language.ENGLISH)
        .vBuild()
