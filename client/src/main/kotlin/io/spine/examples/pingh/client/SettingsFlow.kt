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

import com.google.protobuf.Duration
import io.spine.examples.pingh.github.Username
import io.spine.examples.pingh.sessions.withSession
import io.spine.examples.pingh.sessions.command.LogUserOut
import io.spine.examples.pingh.sessions.event.UserLoggedOut
import io.spine.protobuf.Durations2.hours
import io.spine.protobuf.Durations2.minutes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The application settings control flow.
 *
 * Allows changing application settings and logging out.
 *
 * To persist the settings across application relaunches,
 * use [saveSettings()][saveSettings] method.
 *
 * @property client Enables interaction with the Pingh server.
 * @property session The information about the current user session.
 * @property userSettings The information about the application settings.
 * @property closeSession Updates the application state when a session is closed.
 */
public class SettingsFlow internal constructor(
    private val client: DesktopClient,
    private val session: UserSession,
    private val userSettings: UserSettings,
    private val closeSession: () -> Unit
) {
    /**
     * The state of application settings.
     */
    public val settings: SettingsState = SettingsState(userSettings)

    /**
     * The username to which the current session belongs.
     */
    public val username: Username
        get() = session.username

    /**
     * Logs the user out, cancels all subscriptions and clears the session ID.
     *
     * @param onSuccess Called when the user successfully logs out.
     */
    public fun logOut(onSuccess: (event: UserLoggedOut) -> Unit = {}) {
        val command = LogUserOut::class.withSession(session.id)
        client.observeEvent(command.id, UserLoggedOut::class) { event ->
            closeSession()
            onSuccess(event)
        }
        client.send(command)
    }

    /**
     * Saves the current application setting.
     */
    @Suppress("MemberVisibilityCanBePrivate" /* Accessed from `desktop` module. */)
    public fun saveSettings() {
        userSettings.save()
    }
}

/**
 * State of application settings.
 */
public class SettingsState internal constructor(
    private val data: UserSettings
) {
    private val _enabledDndMode = MutableStateFlow(data.enabledDndMode)
    private val _snoozeTime = MutableStateFlow(data.snoozeTime)

    /**
     * If `true`, the user is not notified about new mentions and snooze expirations.
     * If `false`, the user receives notifications.
     */
    public val enabledDndMode: StateFlow<Boolean> = _enabledDndMode

    /**
     * The interval after which the new mention notification is repeated.
     */
    public val snoozeTime: StateFlow<SnoozeTime> = _snoozeTime

    /**
     * Sets whether the user should NOT receive notifications
     * for new mentions or the expiration of the snooze time.
     */
    public fun setDndMode(isEnabled: Boolean) {
        _enabledDndMode.value = isEnabled
        data.enabledDndMode = isEnabled
    }

    /**
     * Sets the interval after which the new mention notification is repeated.
     */
    public fun setSnoozeTime(snoozeTime: SnoozeTime) {
        _snoozeTime.value = snoozeTime
        data.snoozeTime = snoozeTime
    }
}
