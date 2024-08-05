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

package io.spine.examples.pingh.desktop

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.google.protobuf.Duration
import io.spine.examples.pingh.client.DesktopClient
import io.spine.examples.pingh.github.Username
import io.spine.examples.pingh.sessions.buildBy
import io.spine.examples.pingh.sessions.command.LogUserOut
import io.spine.examples.pingh.sessions.event.UserLoggedOut
import io.spine.protobuf.Durations2.hours
import io.spine.protobuf.Durations2.minutes

internal class SettingsFlow(
    private val client: DesktopClient,
    private val session: MutableState<UserSession?>,
    internal val settings: SettingsState
) {

    internal val username: Username
        get() = session.value!!.username

    /**
     * Logs the user out, cancels all subscriptions and clears the session ID.
     */
    internal fun logOut(
        onSuccess: (event: UserLoggedOut) -> Unit = {}
    ) {
        val command = LogUserOut::class.buildBy(session.value!!.id)
        client.observeEventOnce(command.id, UserLoggedOut::class) { event ->
            session.value = null
            client.asGuest()
            onSuccess(event)
        }
        client.send(command)
    }
}

/**
 * State of application settings.
 */
internal class SettingsState {

    /**
     * If `true`, the user is not notified about new mentions and snooze expirations.
     * If `false`, the user receives notifications.
     */
    internal var enabledDndMode: MutableState<Boolean> = mutableStateOf(false)

    /**
     * The interval after which the new mention notification is repeated.
     */
    internal var snoozeTime: MutableState<SnoozeTime> = mutableStateOf(SnoozeTime.TWO_HOURS)
}

/**
 * Time after which the notification about the new mention is repeated.
 *
 * @param label the text corresponding to this interval.
 * @param value the duration corresponding to this interval.
 */
@Suppress("MagicNumber") // The durations are specified using numbers.
internal enum class SnoozeTime(
    internal val label: String,
    internal val value: Duration
) {
    /**
     * The interval is 30 minutes in duration.
     */
    THIRTY_MINUTES("30 mins", minutes(30)),

    /**
     * The interval is 2 hours in duration.
     */
    TWO_HOURS("2 hours", hours(2)),

    /**
     * The interval is one day in duration.
     */
    ONE_DAY("1 day", hours(24))
}
