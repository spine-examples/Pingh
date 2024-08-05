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
import com.google.protobuf.Duration
import io.spine.examples.pingh.client.DesktopClient
import io.spine.examples.pingh.github.UserCode
import io.spine.examples.pingh.github.Username
import io.spine.examples.pingh.sessions.SessionId
import io.spine.examples.pingh.sessions.buildBy
import io.spine.examples.pingh.sessions.command.LogUserIn
import io.spine.examples.pingh.sessions.command.VerifyUserLoginToGitHub
import io.spine.examples.pingh.sessions.event.UserCodeReceived
import io.spine.examples.pingh.sessions.event.UserIsNotLoggedIntoGitHub
import io.spine.examples.pingh.sessions.event.UserLoggedIn
import io.spine.net.Url

internal class LoginFlow(
    private val client: DesktopClient,
    private val session: MutableState<UserSession?>
) {

    internal lateinit var username: Username
        private set

    internal lateinit var userCode: UserCode
        private set

    internal lateinit var verificationUrl: Url
        private set

    internal lateinit var expiresIn: Duration
        private set

    internal lateinit var interval: Duration
        private set

    /**
     * Starts the login process and requests `UserCode`.
     */
    internal fun requestUserCode(
        username: Username,
        onSuccess: (event: UserCodeReceived) -> Unit = {}
    ) {
        this.username = username
        val command = LogUserIn::class.buildBy(
            SessionId::class.buildBy(username)
        )
        client.observeEventOnce(command.id, UserCodeReceived::class) { event ->
            session.value = UserSession(command.id)
            client.onBehalfOf(session.value!!.userId)
            onSuccess(event)
        }
        client.send(command)
    }

    /**
     * Checks whether the user has completed the login on GitHub and entered their user code.
     */
    internal fun verify(
        onSuccess: (event: UserLoggedIn) -> Unit = {},
        onFail: (event: UserIsNotLoggedIntoGitHub) -> Unit = {}
    ) {
        check(session.value != null) {
            "Initially it is necessary to start the login process by requesting a user code."
        }
        val command = VerifyUserLoginToGitHub::class.buildBy(session.value!!.id)
        client.observeCommandOutcome(
            command.id,
            UserLoggedIn::class,
            onSuccess,
            UserIsNotLoggedIntoGitHub::class,
            onFail
        )
        client.send(command)
    }

    internal fun setVerificationContext(event: UserCodeReceived) {
        userCode = event.userCode
        verificationUrl = event.verificationUrl
        expiresIn = event.expiresIn
        interval = event.interval
    }
}
