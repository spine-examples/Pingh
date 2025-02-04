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

import io.spine.examples.pingh.github.Username
import io.spine.examples.pingh.sessions.SessionId
import io.spine.examples.pingh.sessions.command.LogUserIn
import io.spine.examples.pingh.sessions.event.UserCodeReceived
import io.spine.examples.pingh.sessions.of
import io.spine.examples.pingh.sessions.withSession
import io.spine.logging.Logging

/**
 * A stage of the login flow on which the user enters their GitHub username
 * and receives a user code in return.
 *
 * @property client Enables interaction with the Pingh server.
 * @property moveToNextStage Switches the current stage to the [VerifyLogin].
 */
public class EnterUsername internal constructor(
    private val client: DesktopClient,
    private val moveToNextStage: () -> Unit
) : LoginStage<UserCodeReceived>(), Logging {

    /**
     * Starts the GitHub login process and requests `UserCode`.
     *
     * @param username The username of the user logging in.
     * @param onSuccess Called when the user code is successfully received.
     */
    public fun requestUserCode(
        username: Username,
        onSuccess: (event: UserCodeReceived) -> Unit = {}
    ) {
        val command = LogUserIn::class.withSession(
            SessionId::class.of(username)
        )
        client.observeEvent(command.id, UserCodeReceived::class) { event ->
            result = event
            moveToNextStage()
            onSuccess(event)
            _debug().log("Verification code received.")
        }
        client.send(command)
        _debug().log("Username sent to server; waiting for verification code.")
    }
}
