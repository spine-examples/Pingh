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
import io.spine.client.ConnectionConstants.DEFAULT_CLIENT_SERVICE_PORT
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
 * By default, application opens channel for the client
 * to 'localhost:[50051][DEFAULT_CLIENT_SERVICE_PORT]'.
 *
 * @param address the address of the Pingh server.
 * @param port the port on which the Pingh server is running.
 */
public class PinghApplication(
    address: String = "localhost",
    port: Int = DEFAULT_CLIENT_SERVICE_PORT
) {
    private companion object {
        /**
         * The default amount of seconds to wait
         * when [closing][ManagedChannel.shutdown] the channel.
         */
        private const val defaultShutdownTimeout = 5L
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
     * Asynchronously updates the state of the Pingh application after the [session] is updated.
     *
     * If the `session` is closed:
     * - a guest [client] is created;
     * - the [mentions flow][mentionsFlow] for previous session is deleted.
     *
     * If a new `session` is established:
     * - a `client` is created to make requests on behalf of the user.
     *
     * In all cases, prior to creating a new `client`, all subscriptions of
     * the previous `client` are closed.
     */
    private val sessionObservation = CoroutineScope(Dispatchers.Default).launch {
        session.collect { value ->
            client.close()
            if (value != null) {
                client = DesktopClient(channel, value.asUserId())
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
}

/**
 * Creates a new `UserId` using the username contained in this `UserSession`.
 */
private fun UserSession.asUserId(): UserId =
    UserId.newBuilder()
        .setValue(id.username.value)
        .vBuild()
