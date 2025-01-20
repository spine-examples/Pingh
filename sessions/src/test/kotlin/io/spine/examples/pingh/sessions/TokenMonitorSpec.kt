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

import com.google.protobuf.Timestamp
import io.spine.examples.pingh.clock.buildBy
import io.spine.examples.pingh.clock.event.TimePassed
import io.spine.examples.pingh.github.PersonalAccessToken
import io.spine.examples.pingh.sessions.command.LogUserIn
import io.spine.examples.pingh.sessions.command.UpdateToken
import io.spine.examples.pingh.sessions.command.VerifyUserLoginToGitHub
import io.spine.examples.pingh.sessions.event.SessionExpired
import io.spine.examples.pingh.sessions.event.TokenExpirationTimeUpdated
import io.spine.examples.pingh.sessions.event.TokenMonitoringFinished
import io.spine.examples.pingh.sessions.event.TokenMonitoringStarted
import io.spine.examples.pingh.sessions.event.TokenUpdated
import io.spine.examples.pingh.sessions.event.UserLoggedIn
import io.spine.examples.pingh.sessions.event.UserLoggedOut
import io.spine.examples.pingh.sessions.given.generate
import io.spine.examples.pingh.sessions.given.with
import io.spine.examples.pingh.testing.sessions.given.PredefinedGitHubAuthenticationResponses
import io.spine.examples.pingh.testing.sessions.given.PredefinedGitHubUsersResponses
import io.spine.protobuf.AnyPacker.unpack
import io.spine.protobuf.Durations2.minutes
import io.spine.protobuf.Durations2.seconds
import io.spine.server.BoundedContextBuilder
import io.spine.server.integration.ThirdPartyContext
import io.spine.testing.core.given.GivenUserId
import io.spine.testing.server.blackbox.ContextAwareTest
import java.lang.Thread.sleep
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("`TokenMonitor` should")
internal class TokenMonitorSpec : ContextAwareTest() {

    private val auth = PredefinedGitHubAuthenticationResponses()
    private val users = PredefinedGitHubUsersResponses()

    private lateinit var session: SessionId
    private lateinit var id: TokenMonitorId
    private lateinit var whenExpired: Timestamp

    override fun contextBuilder(): BoundedContextBuilder = newSessionsContext(auth, users)

    @BeforeEach
    internal fun startProcess() {
        session = SessionId::class.generate()
        context().receivesCommand(LogUserIn::class.withSession(session))
        auth.enterUserCode()
        users.username = session.username
        context().receivesCommand(VerifyUserLoginToGitHub::class.withSession(session))
        val loggedInSubject = context().assertEvents()
            .withType(UserLoggedIn::class.java)
        loggedInSubject.hasSize(1)
        val loggedIn = unpack(loggedInSubject.actual()[0].message, UserLoggedIn::class.java)
        id = TokenMonitorId::class.of(session)
        whenExpired = loggedIn.whenTokenExpires
    }

    @AfterEach
    internal fun reset() {
        auth.reset()
        users.reset()
    }

    @Nested internal inner class
    `React on 'UserLoggedIn' event, and` {

        @Test
        internal fun `specify expiration time`() {
            val state = TokenMonitor::class.with(id, whenExpired)
            context().assertState(id, state)
        }

        @Test
        internal fun `emit 'TokenMonitoringStarted' event`() {
            val event = TokenMonitoringStarted::class.with(id)
            context().assertEvent(event)
        }
    }

    @Nested internal inner class
    `React on 'TimePassed' event, and` {

        @Test
        internal fun `do nothing if token doesn't expire`() {
            emitTimePassedEvent(whenExpired.add(minutes(-1)))
            context().assertCommands()
                .withType(UpdateToken::class.java)
                .hasSize(0)
        }

        @Test
        internal fun `send 'UpdateToken' command if token expires`() {
            val time = whenExpired.add(minutes(1))
            emitTimePassedEvent(time)
            val command = UpdateToken::class.with(session, time)
            val subject = context().assertCommands()
                .withType(UpdateToken::class.java)
            subject.hasSize(1)
            subject.message(0)
                .isEqualTo(command)
        }

        @Test
        internal fun `do nothing if update process is in progress`() {
            auth.freezeRefreshing()
            val time = whenExpired.add(minutes(1))
            val firstUpdate = Thread {
                emitTimePassedEvent(time)
            }
            firstUpdate.start()
            sleep(1000)
            context().assertCommands()
                .withType(UpdateToken::class.java)
                .hasSize(1)
            emitTimePassedEvent(time.add(seconds(1)))
            context().assertCommands()
                .withType(UpdateToken::class.java)
                .hasSize(1)
            auth.unfreezeRefreshing()
            firstUpdate.join()
        }

        @Test
        internal fun `do nothing if user is logged out`() {
            context().receivesEvent(UserLoggedOut::class.with(session))
            val time = whenExpired.add(minutes(1))
            emitTimePassedEvent(time)
            context().assertCommands()
                .withType(UpdateToken::class.java)
                .hasSize(0)
        }

        @Test
        internal fun `do nothing if session has expired`() {
            context().receivesEvent(SessionExpired::class.with(session))
            val time = whenExpired.add(minutes(1))
            emitTimePassedEvent(time)
            context().assertCommands()
                .withType(UpdateToken::class.java)
                .hasSize(0)
        }

        private fun emitTimePassedEvent(time: Timestamp) {
            val clockContext = ThirdPartyContext.singleTenant("Clock")
            val event = TimePassed::class.buildBy(time)
            val actor = GivenUserId.generated()
            clockContext.emittedEvent(event, actor)
        }
    }

    @Nested internal inner class
    `React on 'TokenUpdated' event, and` {

        @BeforeEach
        internal fun emit() {
            whenExpired = whenExpired.add(minutes(1))
            val event = TokenUpdated::class.with(
                session, PersonalAccessToken::class.generate(), whenExpired
            )
            context().receivesEvent(event)
        }

        @Test
        internal fun `update the expiration time and finish the token update process`() {
            val state = TokenMonitor::class.with(id, whenExpired)
            context().assertState(id, state)
        }

        @Test
        internal fun `emit 'TokenExpirationTimeUpdated' event`() {
            val event = TokenExpirationTimeUpdated::class.with(id)
            context().assertEvent(event)
        }
    }

    @Nested internal inner class
    `React on 'UserLoggedOut' event, and` {

        @BeforeEach
        internal fun emit() {
            val event = UserLoggedOut::class.with(session)
            context().receivesEvent(event)
        }

        @Test
        internal fun `delete process state`() {
            context().assertEntity(id, TokenMonitorProcess::class.java)
                .deletedFlag()
                .isTrue()
        }

        @Test
        internal fun `emit 'TokenMonitoringFinished' event`() {
            val event = TokenMonitoringFinished::class.with(id)
            context().assertEvent(event)
        }
    }

    @Nested internal inner class
    `React on 'SessionExpired' event, and` {

        @BeforeEach
        internal fun emit() {
            val event = SessionExpired::class.with(session)
            context().receivesEvent(event)
        }

        @Test
        internal fun `delete process state`() {
            context().assertEntity(id, TokenMonitorProcess::class.java)
                .deletedFlag()
                .isTrue()
        }

        @Test
        internal fun `emit 'TokenMonitoringFinished' event`() {
            val event = TokenMonitoringFinished::class.with(id)
            context().assertEvent(event)
        }
    }
}
