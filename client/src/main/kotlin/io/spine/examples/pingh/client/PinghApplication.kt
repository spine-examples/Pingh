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

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.spine.core.UserId
import io.spine.examples.pingh.mentions.MentionStatus
import io.spine.examples.pingh.sessions.SessionId
import io.spine.examples.pingh.sessions.command.VerifySession
import io.spine.examples.pingh.sessions.event.SessionExpired
import io.spine.examples.pingh.sessions.event.SessionVerificationFailed
import io.spine.examples.pingh.sessions.event.SessionVerified
import io.spine.examples.pingh.sessions.with
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Manages the logic for the Pingh app.
 *
 * Stores application states and allows to create different process flows.
 *
 * @param notificationSender Allows to send notifications.
 * @param address The address of the Pingh server.
 * @param port The port on which the Pingh server is running.
 */
public class PinghApplication private constructor(
    notificationSender: NotificationSender,
    address: String,
    port: Int
) {
    public companion object {
        /**
         * The default amount of seconds to wait
         * when [closing][ManagedChannel.shutdown] the channel.
         */
        private const val defaultShutdownTimeout = 5L

        /**
         * Creates a new builder for Pingh application.
         */
        public fun builder(): Builder = Builder()
    }

    /**
     * Channel for the communication with the Pingh server.
     */
    private val channel = ManagedChannelBuilder
        .forAddress(address, port)
        .usePlaintext()
        .build()

    /**
     * Enables interaction with the Pingh server.
     */
    internal var client: DesktopClient = DesktopClient(channel)
        private set

    /**
     * Manages the session with Pingh server.
     */
    private val session: Session

    /**
     * Manages the application settings configured by a user.
     */
    private val settings: Settings

    init {
        val storage = UserDataStorage()
        session = Session(storage)
        settings = Settings(storage)

        // Resets a locally saved session if it is no longer active.
        if (session.isActive) {
            if (!client.verifySession(session.id)) {
                session.resetToGuest()
            }
        }

        if (session.isActive) {
            client = DesktopClient(channel, session.id.asUserId())
        }
    }

    /**
     * Describes the login to GitHub via GitHub's device flow.
     */
    private var loginFlow: LoginFlow? = null

    /**
     * Controls the lifecycle of mentions and handles the user's action in relation to them.
     */
    private var mentionsFlow: MentionsFlow? = null

    private val _unreadMentionCount: MutableStateFlow<Int?> = MutableStateFlow(null)

    /**
     * The count of unread mentions for the user,
     * or `null` if the user is not logged in.
     */
    public val unreadMentionCount: StateFlow<Int?> = _unreadMentionCount

    private val _loggedIn: MutableStateFlow<Boolean> = MutableStateFlow(session.isActive)

    /**
     * Whether the user is logged into the application.
     *
     * If `true`, the user has an active session.
     */
    public val loggedIn: StateFlow<Boolean> = _loggedIn

    /**
     * A job that updates the unread mention count
     * whenever the state of a user's mentions changes.
     */
    private var mentionsObserver: Job? = null

    /**
     * The application settings control flow.
     */
    private var settingsFlow: SettingsFlow? = null

    /**
     * Flow that manages the sending of notifications within the app.
     */
    private val notificationsFlow = NotificationsFlow(notificationSender, settings)

    init {
        if (session.isActive) {
            notificationsFlow.enableNotifications(client, session.username)
        }
    }

    /**
     * Updates the application state when a session is established:
     *
     * - a [client] is created to make requests on behalf of the user;
     * - notifications are enabled for the newly created client.
     */
    private fun establishSession(id: SessionId) {
        client.close()
        session.establish(id)
        client = DesktopClient(channel, id.asUserId())
        notificationsFlow.enableNotifications(client, id.username)
        subscribeToSessionExpiration(id)
        _loggedIn.value = true
    }

    /**
     * Updates the application state if the session in use has expired.
     */
    private fun subscribeToSessionExpiration(id: SessionId) {
        client.observeEvent(id, SessionExpired::class) {
            closeSession()
            notificationsFlow.send(
                "Pingh",
                "Your session has expired.${System.lineSeparator()}Please log in again."
            )
        }
    }

    /**
     * Updates the application state when a session is closed:
     *
     * - a guest [client] is created;
     * - the [mentions flow][mentionsFlow] for previous session is deleted.
     */
    private fun closeSession() {
        _loggedIn.value = false
        client.close()
        session.resetToGuest()
        client = DesktopClient(channel)
        mentionsFlow = null
        _unreadMentionCount.value = null
        mentionsObserver?.cancel()
        settingsFlow = null
    }

    /**
     * Initiates the login flow and terminates any previous flow, if it exists.
     */
    public fun startLoginFlow(): LoginFlow {
        if (loginFlow == null || loginFlow!!.isCompleted()) {
            loginFlow = LoginFlow(client, ::establishSession)
        }
        return loginFlow!!
    }

    /**
     * Returns mentions flow for current session.
     *
     * If the mentions flow does not already exist, it is initialized.
     */
    public fun startMentionsFlow(): MentionsFlow {
        if (mentionsFlow == null) {
            mentionsFlow = MentionsFlow(client, session, settings)
            observeMentions()
        } else {
            mentionsFlow!!.applySettings()
        }
        return mentionsFlow!!
    }

    /**
     * Observes the state of a user's mentions
     * and updates the unread mention count whenever changes occur.
     */
    private fun observeMentions() {
        mentionsObserver = CoroutineScope(Dispatchers.Default).launch {
            mentionsFlow!!.mentions.collect { mentions ->
                _unreadMentionCount.value = mentions.count { it.status == MentionStatus.UNREAD }
            }
        }
    }

    /**
     * Initiates the settings flow.
     *
     * If the settings flow does not already exist, it is initialized.
     */
    public fun startSettingsFlow(): SettingsFlow {
        if (settingsFlow == null) {
            settingsFlow = SettingsFlow(client, session, settings, ::closeSession)
        }
        return settingsFlow!!
    }

    /**
     * Closes the client.
     */
    public fun close() {
        mentionsObserver?.cancel()
        loginFlow?.close()
        settingsFlow?.saveSettings()
        client.close()
        channel.shutdown()
            .awaitTermination(defaultShutdownTimeout, TimeUnit.SECONDS)
    }

    /**
     * A builder for [PinghApplication].
     */
    public class Builder {
        /**
         * The address of the Pingh server.
         */
        private var address: String? = null

        /**
         * The port on which the Pingh server is running.
         */
        private var port: Int? = null

        /**
         * Allows to send notifications.
         */
        private var notificationSender: NotificationSender? = null

        /**
         * Sets the address of the Pingh server.
         */
        public fun withAddress(address: String): Builder {
            this.address = address
            return this
        }

        /**
         * Sets the port on which the Pingh server is running.
         */
        public fun withPort(port: Int): Builder {
            this.port = port
            return this
        }

        /**
         * Sets sender for notifications.
         */
        public fun with(notificationSender: NotificationSender): Builder {
            this.notificationSender = notificationSender
            return this
        }

        /**
         * Builds a Pingh application with configured data.
         *
         * @throws IllegalStateException if some application data is missing.
         */
        public fun build(): PinghApplication {
            checkNotNull(notificationSender) { "Notification sender must be specified." }
            checkNotNull(address) { "Address must be specified." }
            checkNotNull(port) { "Port must be specified." }
            return PinghApplication(notificationSender!!, address!!, port!!)
        }
    }
}

/**
 * Creates a new `UserId` using the username contained in this `SessionId`.
 */
private fun SessionId.asUserId(): UserId =
    UserId.newBuilder()
        .setValue(username.value)
        .vBuild()

/**
 * Returns `true` if session is active.
 */
private fun DesktopClient.verifySession(id: SessionId): Boolean {
    val future = CompletableFuture<Boolean>()
    observeEither(
        EventObserver(id, SessionVerified::class) {
            future.complete(true)
        },
        EventObserver(id, SessionVerificationFailed::class) {
            future.complete(false)
        }
    )
    send(VerifySession::class.with(id))
    return try {
        future.get(2, TimeUnit.SECONDS)
    } catch (ignore: TimeoutException) {
        false
    }
}
