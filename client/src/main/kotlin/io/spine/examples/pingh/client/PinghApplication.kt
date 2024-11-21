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
import io.spine.examples.pingh.sessions.SessionId
import java.util.concurrent.TimeUnit

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
     * Manages the local data for users of the application.
     */
    private val local = UserDataManager()

    /**
     * Enables interaction with the Pingh server.
     */
    internal var client: DesktopClient
        private set

    init {
        client = if (local.isLoggedIn) {
            DesktopClient(channel, local.session.asUserId())
        } else {
            DesktopClient(channel)
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

    /**
     * The application settings control flow.
     */
    private var settingsFlow: SettingsFlow? = null

    /**
     * Flow that manages the sending of notifications within the app.
     */
    private val notificationsFlow = NotificationsFlow(notificationSender, local)

    init {
        if (local.isLoggedIn) {
            notificationsFlow.enableNotifications(client, local.user)
        }
    }

    /**
     * Updates the application state when a session is established:
     *
     * - a [client] is created to make requests on behalf of the user;
     * - notifications are enabled for the newly created client.
     */
    private fun establishSession(session: SessionId) {
        client.close()
        local.establish(session)
        client = DesktopClient(channel, session.asUserId())
        notificationsFlow.enableNotifications(client, session.username)
    }

    /**
     * Updates the application state when a session is closed:
     *
     * - a guest [client] is created;
     * - the [mentions flow][mentionsFlow] for previous session is deleted.
     */
    private fun closeSession() {
        client.close()
        local.resetToGuest()
        client = DesktopClient(channel)
        mentionsFlow = null
        settingsFlow = null
    }

    /**
     * Returns `true` if the user is logged in to the application.
     */
    public fun isLoggedIn(): Boolean = local.isLoggedIn

    /**
     * Initiates the login flow and terminates any previous flow, if it exists.
     */
    public fun startLoginFlow(): LoginFlow {
        loginFlow?.close()
        loginFlow = LoginFlow(client, local, ::establishSession)
        return loginFlow!!
    }

    /**
     * Returns mentions flow for current session.
     *
     * If the mentions flow does not already exist, it is initialized.
     */
    public fun startMentionsFlow(): MentionsFlow {
        if (mentionsFlow == null) {
            mentionsFlow = MentionsFlow(client, local)
        }
        return mentionsFlow!!
    }

    /**
     * Initiates the settings flow.
     *
     * If the settings flow does not already exist, it is initialized.
     */
    public fun startSettingsFlow(): SettingsFlow {
        if (settingsFlow == null) {
            settingsFlow = SettingsFlow(client, local, ::closeSession)
        }
        return settingsFlow!!
    }

    /**
     * Closes the client.
     */
    public fun close() {
        loginFlow?.close()
        settingsFlow?.saveSettings()
        local.clear()
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
