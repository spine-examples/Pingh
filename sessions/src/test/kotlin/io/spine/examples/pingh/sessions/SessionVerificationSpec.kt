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

package io.spine.examples.pingh.sessions

import io.spine.examples.pingh.sessions.event.ActiveSessionAdded
import io.spine.examples.pingh.sessions.event.InactiveSessionRemoved
import io.spine.examples.pingh.sessions.event.SessionExpired
import io.spine.examples.pingh.sessions.event.SessionVerificationFailed
import io.spine.examples.pingh.sessions.event.SessionVerified
import io.spine.examples.pingh.sessions.event.UserLoggedIn
import io.spine.examples.pingh.sessions.event.UserLoggedOut
import io.spine.examples.pingh.sessions.event.VerifySession
import io.spine.examples.pingh.sessions.given.generate
import io.spine.examples.pingh.sessions.given.generateWith
import io.spine.examples.pingh.sessions.given.with
import io.spine.examples.pingh.testing.sessions.given.PredefinedGitHubAuthenticationResponses
import io.spine.examples.pingh.testing.sessions.given.PredefinedGitHubUsersResponses
import io.spine.protobuf.Durations2.minutes
import io.spine.server.BoundedContextBuilder
import io.spine.testing.server.blackbox.ContextAwareTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("`SessionVerification` should")
internal class SessionVerificationSpec : ContextAwareTest() {

    private lateinit var id: SessionVerificationId
    private lateinit var activeSession: SessionId

    override fun contextBuilder(): BoundedContextBuilder = newSessionsContext(
        PredefinedGitHubAuthenticationResponses(),
        PredefinedGitHubUsersResponses()
    )

    @BeforeEach
    internal fun logUserIn() {
        activeSession = SessionId::class.generate()
        id = SessionVerificationId::class.of(activeSession.username)
        val event = UserLoggedIn::class.generateWith(activeSession)
        context().receivesEvent(event)
    }

    @Nested internal inner class
    `React on 'UserLoggedIn' event, and` {

        @Test
        internal fun `emit 'ActiveSessionAdded' event`() {
            val expected = ActiveSessionAdded::class.with(id)
            context().assertEvent(expected)
        }

        @Test
        internal fun `add new session to list of active sessions`() {
            val expected = SessionVerification::class.with(id, activeSession)
            context().assertState(id, expected)
        }
    }

    @Nested internal inner class
    `Handle 'VerifySession' command, and` {

        @Test
        internal fun `emit 'SessionVerified' if active session verifies`() {
            val command = VerifySession::class.with(id, activeSession)
            context().receivesCommand(command)
            val expired = SessionVerified::class.with(id)
            context().assertEvent(expired)
        }

        @Test
        internal fun `emit 'SessionVerificationFailed' if inactive session verifies`() {
            val inactiveSession = activeSession.run {
                SessionId::class.of(username, whenCreated.add(minutes(1)))
            }
            val command = VerifySession::class.with(id, inactiveSession)
            context().receivesCommand(command)
            val expired = SessionVerificationFailed::class.with(id)
            context().assertEvent(expired)
        }
    }

    @Nested internal inner class
    `React on 'UserLoggedOut' event, and` {

        @BeforeEach
        internal fun logUserOut() {
            val event = UserLoggedOut::class.with(activeSession)
            context().receivesEvent(event)
        }

        @Test
        internal fun `emit 'InactiveSessionRemoved' event`() {
            val expected = InactiveSessionRemoved::class.with(id)
            context().assertEvent(expected)
        }

        @Test
        internal fun `remove inactive session from list of active sessions`() {
            val expected = SessionVerification::class.with(id)
            context().assertState(id, expected)
        }
    }

    @Nested internal inner class
    `React on 'SessionExpires' event, and` {

        @BeforeEach
        internal fun expireSession() {
            val event = SessionExpired::class.with(activeSession)
            context().receivesEvent(event)
        }

        @Test
        internal fun `emit 'InactiveSessionRemoved' event`() {
            val expected = InactiveSessionRemoved::class.with(id)
            context().assertEvent(expected)
        }

        @Test
        internal fun `remove inactive session from list of active sessions`() {
            val expected = SessionVerification::class.with(id)
            context().assertState(id, expected)
        }
    }
}
