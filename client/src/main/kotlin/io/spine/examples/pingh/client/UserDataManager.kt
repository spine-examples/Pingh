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
import io.spine.examples.pingh.client.settings.SnoozeTime
import io.spine.examples.pingh.client.settings.UserSettings
import io.spine.examples.pingh.github.Username
import io.spine.examples.pingh.sessions.SessionId
import kotlin.reflect.KClass

/**
 * Manages the local data for users of the application.
 *
 * Provides details of the [session] and [settings] for each specific [user][name].
 *
 * Upon creation, retrieves the local data of the user who was logged in during
 * the last startup. If no user was logged in, it defaults to guest data,
 * which is replaced when a session is established with the server.
 *
 * All changes made for a logged-in user are saved to a file in the user's data directory,
 * ensuring they persist across application restarts.
 */
internal class UserDataManager {
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
    private var data: UserData

    init {
        data = registry.dataList
            .firstOrNull { it.hasSession() }
            ?: guestData
    }

    /**
     * A name of the user currently using the application.
     */
    internal val name: Username
        get() = data.user

    /**
     * An identifier of the current session with the Pingh server.
     */
    internal val session: SessionId
        get() = data.session

    /**
     * An application settings applied by this user.
     */
    internal val settings: UserSettings
        get() = data.settings

    /**
     * Whether the current user is logged in to the application.
     */
    internal val loggedIn: Boolean
        get() = data.hasSession()

    /**
     * Sets a new session for the user.
     *
     * Searches for the user's local data in the [registry].
     * If it does not exist, creates the user data for the session
     * and sets the [default] settings.
     */
    internal fun establish(session: SessionId) {
        data = registry.dataList
            .firstOrNull { it.user.equals(session.username) }
            ?.toBuilder()
            ?.setSession(session)
            ?.vBuild()
            ?: userDataFor(session)
        save()
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
        modifyData {
            clearSession()
        }
        data = guestData
    }

    /**
     * Sets and saves updated application settings.
     */
    internal fun update(settings: UserSettings) {
        modifyData {
            this.settings = settings
        }
    }

    /**
     * Applies the `modifier` to the builder of the current local [data]
     * and saves the resulting data.
     */
    private fun modifyData(modifier: UserData.Builder.() -> Unit) {
        data = with(data.toBuilder()) {
            modifier()
            vBuild()
        }
        save()
    }

    /**
     * Updates the current [data] in the [registry] and saves it.
     */
    private fun save() {
        if (data.user.equals(guest)) {
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

    private companion object {
        private val guest: Username = Username.newBuilder()
            .setValue("Guest username")
            // Uses invalid GitHub username and avoids validation
            // so that there is no collision with real usernames.
            .buildPartial()

        private val guestData: UserData =
            UserData.newBuilder()
                .setUser(guest)
                .setSettings(UserSettings::class.default())
                .vBuild()
    }
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
        .vBuild()
