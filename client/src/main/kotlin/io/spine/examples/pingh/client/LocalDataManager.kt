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
 * Provides details of the [session] and [settings] for each specific [user].
 *
 * The manager can be in one of two states:
 *
 * 1. Guest: No user is logged in. In this state, the user must log in
 *   and does not have access to mentions or settings.
 *
 * 2. Authenticated: A user is logged in. In this state,
 *   the user has access to mentions and settings.
 *
 * The current state can be checked using the [isGuest()][isGuest] method.
 *
 * All changes are saved to a file in the user's data directory,
 * ensuring persistence across application restarts.
 */
internal class LocalDataManager {
    /**
     * A repository for storing local data for all users.
     */
    private val storage = FileStorage<LocalDataRegistry>(inAppDir(".local-data-registry"))

    /**
     * A local data for all users who have used this app on a specific device.
     */
    private val registry: LocalDataRegistry =
        storage.loadOrDefault(LocalDataRegistry::parseFrom) { LocalDataRegistry::class.empty() }

    /**
     * A local data specific to a user, stored on the device.
     */
    private var data: LocalData

    init {
        data = registry.dataList
            .firstOrNull { it.hasSession() }
            ?: guestData
    }

    /**
     * A name of the user currently using the application.
     */
    internal val user: Username
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
     * Sets a new session for the user.
     *
     * Searches for the user's local data in the [registry].
     * If it does not exist, creates the local data for the session
     * and sets the [default] settings.
     */
    internal fun establish(session: SessionId) {
        data = registry.dataList
            .firstOrNull { it.user.equals(session.username) }
            ?.toBuilder()
            ?.setSession(session)
            ?.vBuild()
            ?: localDataFor(session)
        save()
    }

    private fun localDataFor(session: SessionId): LocalData =
        LocalData.newBuilder()
            .setUser(session.username)
            .setSession(session)
            .setSettings(UserSettings::class.default())
            .vBuild()

    /**
     * Clears the current user session
     * and replaces the local data with guest one.
     */
    internal fun resetToGuest() {
        data = data.toBuilder()
            .clearSession()
            .vBuild()
        save()
        data = guestData
    }

    /**
     * Returns `true` if the current [session] is a guest session.
     */
    internal fun isGuest(): Boolean = !data.hasSession()

    /**
     * Sets and saves updated application settings.
     */
    internal fun update(settings: UserSettings) {
        data = data.toBuilder()
            .setSettings(settings)
            .vBuild()
        save()
    }

    /**
     * Updates the current [data] in the [registry] and saves it.
     */
    private fun save() {
        if (data == guestData) {
            return
        }
        val id = registry.dataList.indexOfFirst { it.user.equals(user) }
        if (id == -1) {
            registry.dataList.add(data)
        } else {
            registry.dataList[id] = data
        }
        storage.save(registry)
    }

    private companion object {
        private val guest: Username = Username.newBuilder()
            .setValue("Guest username")
            // Uses invalid GitHub username and avoids validation
            // so that there is no collision with real usernames.
            .buildPartial()

        private val guestData: LocalData =
            LocalData.newBuilder()
                .setUser(guest)
                .setSettings(UserSettings::class.default())
                .vBuild()
    }
}

/**
 * Creates a new `LocalDataRegistry` with an empty data list.
 */
private fun KClass<LocalDataRegistry>.empty(): LocalDataRegistry =
    LocalDataRegistry.newBuilder().vBuild()

/**
 * Create a new `UserSettings` with default settings values.
 */
private fun KClass<UserSettings>.default(): UserSettings =
    UserSettings.newBuilder()
        .setEnabledDndMode(false)
        .setSnoozeTime(SnoozeTime.TWO_HOURS)
        .vBuild()
