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

import io.spine.base.EventMessage
import io.spine.examples.pingh.sessions.command.LogUserIn
import io.spine.examples.pingh.sessions.command.LogUserOut
import io.spine.examples.pingh.sessions.event.UserLoggedIn
import io.spine.examples.pingh.sessions.event.UserLoggedOut
import io.spine.examples.pingh.sessions.given.buildBy
import io.spine.examples.pingh.sessions.given.buildWithoutToken
import io.spine.examples.pingh.sessions.given.generate
import io.spine.examples.pingh.testing.sessions.given.PredefinedGitHubAuthenticationResponses
import io.spine.server.BoundedContextBuilder
import io.spine.testing.server.EventSubject
import io.spine.testing.server.blackbox.ContextAwareTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("Sessions Context should")
public class SessionsContextSpec : ContextAwareTest() {

    protected override fun contextBuilder(): BoundedContextBuilder =
        newSessionsContext(PredefinedGitHubAuthenticationResponses())

    @Nested
    public inner class `handle 'LogUserIn' command, and` {

        private lateinit var sessionId: SessionId

        @BeforeEach
        public fun sendCommand() {
            sessionId = SessionId::class.generate()
            val command = LogUserIn::class.buildBy(sessionId)
            context().receivesCommand(command)
        }

        @Test
        public fun `emit 'UserLoggedIn' event`() {
            val expected = UserLoggedIn::class.buildWithoutToken(sessionId)
            val events = assertEvents(UserLoggedIn::class.java)
            events.hasSize(1)
            events.message(0)
                .comparingExpectedFieldsOnly()
                .isEqualTo(expected)
        }

        @Test
        public fun `update 'UserSession' entity`() {
            val expected = UserSession::class.buildBy(sessionId)
            context().assertState(sessionId, expected)
        }
    }

    @Nested
    public inner class `handle 'LogUserOut' command, and` {

        private lateinit var sessionId: SessionId

        @BeforeEach
        public fun sendCommand() {
            sessionId = SessionId::class.generate()
            context()
                .receivesCommand(LogUserIn::class.buildBy(sessionId))
                .receivesCommand(LogUserOut::class.buildBy(sessionId))
        }

        @Test
        public fun `emit 'UserLoggedOut' event`() {
            val expected = UserLoggedOut::class.buildBy(sessionId)
            context().assertEvent(expected)
        }

        @Test
        public fun `delete 'UserSession' entity`() {
            context().assertEntity(sessionId, UserSessionProcess::class.java)
                .deletedFlag()
                .isTrue()
        }
    }

    @Test
    public fun `support simultaneous sessions`() {
        val firstSession = SessionId::class.generate()
        val secondSession = SessionId::class.buildBy(firstSession.username)
        context()
            .receivesCommand(LogUserIn::class.buildBy(firstSession))
            .receivesCommand(LogUserIn::class.buildBy(secondSession))

        val firstExpected = UserSession::class.buildBy(firstSession)
        val secondExpected = UserSession::class.buildBy(secondSession)
        context().assertState(firstSession, firstExpected)
        context().assertState(secondSession, secondExpected)
    }

    @Test
    public fun `create new session when user logs in again`() {
        val firstSession = SessionId::class.generate()
        val secondSession = SessionId::class.buildBy(firstSession.username)
        context()
            .receivesCommand(LogUserIn::class.buildBy(firstSession))
            .receivesCommand(LogUserOut::class.buildBy(firstSession))
            .receivesCommand(LogUserIn::class.buildBy(secondSession))

        val secondExpected = UserSession::class.buildBy(secondSession)
        context().assertEntity(firstSession, UserSessionProcess::class.java)
            .deletedFlag()
            .isTrue()
        context().assertState(secondSession, secondExpected)
    }

    /**
     * Checks for events of the provided type emitted by the bounded context under the test.
     */
    private fun <T : EventMessage> assertEvents(eventClass: Class<T>): EventSubject =
        context().assertEvents()
            .withType(eventClass)
}
