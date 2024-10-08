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
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
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
    internal var client = DesktopClient(channel)
        private set

    /**
     * State of application settings.
     */
    private val settings = SettingsState()

    /**
     * Information about the current user session.
     */
    private val session = MutableStateFlow<UserSession?>(null)

    /**
     * Controls the lifecycle of mentions and handles the user's action in relation to them.
     */
    private var mentionsFlow: MentionsFlow? = null

    /**
     * Flow that manages the sending of notifications within the app.
     */
    private val notificationsFlow = NotificationsFlow(notificationSender, settings)

    /**
     * Asynchronously updates the state of the Pingh application after the [session] is updated.
     *
     * If the `session` is closed:
     * - a guest [client] is created;
     * - the [mentions flow][mentionsFlow] for previous session is deleted.
     *
     * If a new `session` is established:
     * - a `client` is created to make requests on behalf of the user;
     * - notifications are enabled for the newly created client.
     *
     * In all cases, prior to creating a new `client`, all subscriptions of
     * the previous `client` are closed.
     */
    private val sessionObservation = CoroutineScope(Dispatchers.Default).launch {
        session.collect { value ->
            client.close()
            if (value != null) {
                client = DesktopClient(channel, value.asUserId())
                notificationsFlow.enableNotifications(client, value.username)
            } else {
                client = DesktopClient(channel)
                mentionsFlow = null
            }
        }
    }

    /**
     * Returns `true` if a user session exists, otherwise `false`.
     */
    public fun isLoggedIn(): Boolean = session.value != null

    /**
     * Initiates the login flow.
     */
    public fun startLoginFlow(): LoginFlow = LoginFlow(client, session)

    /**
     * Returns mentions flow for current session.
     *
     * If the mentions flow does not already exist, it is initialized.
     */
    public fun startMentionsFlow(): MentionsFlow {
        if (mentionsFlow == null) {
            mentionsFlow = MentionsFlow(client, session, settings)
        }
        return mentionsFlow!!
    }

    /**
     * Initiates the settings flow.
     */
    public fun startSettingsFlow(): SettingsFlow = SettingsFlow(client, session, settings)

    /**
     * Closes the client.
     */
    public fun close() {
        sessionObservation.cancel()
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
 * Creates a new `UserId` using the username contained in this `UserSession`.
 */
private fun UserSession.asUserId(): UserId =
    UserId.newBuilder()
        .setValue(id.username.value)
        .vBuild()
