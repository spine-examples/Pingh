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

import androidx.compose.runtime.mutableStateOf
import io.spine.client.ConnectionConstants.DEFAULT_CLIENT_SERVICE_PORT

/**
 * Manages the logic for the Pingh app.
 *
 * Stores application states and allows to create different process flows.
 *
 * By default, application opens channel for the client
 * to 'localhost:[50051][DEFAULT_CLIENT_SERVICE_PORT]'.
 */
public class PinghApplication(
    address: String = "localhost",
    port: Int = DEFAULT_CLIENT_SERVICE_PORT
) {
    /**
     * Enables interaction with the Pingh server.
     */
    internal val client = DesktopClient(address, port)

    /**
     * State of application settings.
     */
    private val settings = SettingsState()

    /**
     * Information about the current user session.
     */
    private val session = mutableStateOf<UserSession?>(null)

    /**
     * Returns `true` if a user session exists, otherwise `false`.
     */
    public fun isLoggedIn(): Boolean = session.value != null

    /**
     * Initiates the login flow.
     */
    public fun startLoginFlow(): LoginFlow = LoginFlow(client, session)

    /**
     * Initiates the mentions flow.
     */
    public fun startMentionsFlow(): MentionsFlow = MentionsFlow(client, session, settings)

    /**
     * Initiates the settings flow.
     */
    public fun startSettingsFlow(): SettingsFlow = SettingsFlow(client, session, settings)

    /**
     * Closes the client by shutting down the gRPC connection.
     */
    public fun close() {
        client.close()
    }
}
