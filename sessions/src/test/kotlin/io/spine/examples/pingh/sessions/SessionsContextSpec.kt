/*
 * Copyright 2024, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import io.kotest.matchers.shouldBe
import io.spine.base.EventMessage
import io.spine.examples.pingh.sessions.event.UserLoggedIn
import io.spine.examples.pingh.sessions.event.UserLoggedOut
import io.spine.examples.pingh.sessions.given.*
import io.spine.protobuf.AnyPacker
import io.spine.server.BoundedContextBuilder
import io.spine.testing.server.EventSubject
import io.spine.testing.server.blackbox.ContextAwareTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("Sessions Context should")
class SessionsContextSpec : ContextAwareTest() {

    override fun contextBuilder(): BoundedContextBuilder =
        newBuilder()

    @Nested
    inner class `handle the 'LogUserIn' command` {

        private lateinit var session: SessionId

        @BeforeEach
        fun sendCommand() {
            session = createSession()
            val command = logUserIn(session)
            context().receivesCommand(command)
        }

        @Test
        fun `emit 'UserLoggedIn' event`() {
            val expected = with(UserLoggedIn.newBuilder()) {
                id = session
                build()
            }
            val events = assertEvents(UserLoggedIn::class.java)
            events.hasSize(1)
            events.message(0)
                .comparingExpectedFieldsOnly()
                .isEqualTo(expected)
        }

        @Test
        fun `update the 'UserSession' entity`() {
            val expected = userSession(session)
            context().assertState(session, expected)
        }
    }

    @Nested
    inner class `handle the 'LogUserOut' command` {

        private lateinit var session: SessionId

        @BeforeEach
        fun sendCommand() {
            session = createSession()
            context().receivesCommand(logUserIn(session))
            context().receivesCommand(logUserOut(session))
        }

        @Test
        fun `emit 'UserLoggedOut' event`() {
            val expected = with(UserLoggedOut.newBuilder()) {
                id = session
                vBuild()
            }
            val eventSubject = assertEvents(UserLoggedOut::class.java)
            eventSubject.hasSize(1)
            val event = eventSubject.actual()[0]
            val message = AnyPacker.unpack(event.message, UserLoggedOut::class.java)
            message shouldBe expected
        }

        @Test
        fun `delete 'UserSession' entity`() {
            context().assertEntity(session, UserSessionProcess::class.java)
                .deletedFlag()
                .isTrue()
        }
    }

    @Test
    fun `support multi-session`() {
        val firstSession = createSession()
        val secondSession = createSessionBy(firstSession.username)
        context().receivesCommand(logUserIn(firstSession))
        context().receivesCommand(logUserIn(secondSession))

        val firstExpected = userSession(firstSession)
        val secondExpected = userSession(secondSession)
        context().assertState(firstSession, firstExpected)
        context().assertState(secondSession, secondExpected)
    }

    @Test
    fun `create new session when user logs in again`() {
        val firstSession = createSession()
        val secondSession = createSessionBy(firstSession.username)
        context().receivesCommand(logUserIn(firstSession))
        context().receivesCommand(logUserOut(firstSession))
        context().receivesCommand(logUserIn(secondSession))

        val secondExpected = userSession(secondSession)
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
