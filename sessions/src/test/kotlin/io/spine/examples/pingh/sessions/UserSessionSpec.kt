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
import com.google.protobuf.util.Timestamps.between
import io.kotest.matchers.shouldBe
import io.spine.base.Time.currentTime
import io.spine.examples.pingh.clock.buildBy
import io.spine.examples.pingh.clock.event.TimePassed
import io.spine.examples.pingh.github.Username
import io.spine.examples.pingh.github.of
import io.spine.examples.pingh.sessions.UserSessionProcess.Companion.lifetime
import io.spine.examples.pingh.sessions.UserSessionProcess.Companion.maxLoginTime
import io.spine.examples.pingh.sessions.command.LogUserIn
import io.spine.examples.pingh.sessions.command.LogUserOut
import io.spine.examples.pingh.sessions.command.UpdateToken
import io.spine.examples.pingh.sessions.command.VerifySession
import io.spine.examples.pingh.sessions.command.VerifyUserLoginToGitHub
import io.spine.examples.pingh.sessions.event.SessionClosed
import io.spine.examples.pingh.sessions.event.SessionExpired
import io.spine.examples.pingh.sessions.event.SessionVerificationFailed
import io.spine.examples.pingh.sessions.event.SessionVerified
import io.spine.examples.pingh.sessions.event.TokenUpdated
import io.spine.examples.pingh.sessions.event.UserIsNotLoggedIntoGitHub
import io.spine.examples.pingh.sessions.event.UserLoggedIn
import io.spine.examples.pingh.sessions.event.UserLoggedOut
import io.spine.examples.pingh.sessions.given.expectedTokenUpdatedEvent
import io.spine.examples.pingh.sessions.given.with
import io.spine.examples.pingh.sessions.given.expectedUserCodeReceivedEvent
import io.spine.examples.pingh.sessions.given.expectedUserLoggedInEvent
import io.spine.examples.pingh.sessions.given.expectedUserSessionAfterTokenUpdate
import io.spine.examples.pingh.sessions.given.expectedUserSessionWithDeviceCode
import io.spine.examples.pingh.sessions.given.expectedUserSessionWithRefreshToken
import io.spine.examples.pingh.sessions.given.generate
import io.spine.examples.pingh.sessions.rejection.Rejections.NotMemberOfPermittedOrgs
import io.spine.examples.pingh.sessions.rejection.Rejections.UsernameMismatch
import io.spine.examples.pingh.testing.sessions.given.PredefinedGitHubAuthenticationResponses
import io.spine.examples.pingh.testing.sessions.given.PredefinedGitHubUsersResponses
import io.spine.protobuf.AnyPacker
import io.spine.protobuf.Durations2.minutes
import io.spine.protobuf.Durations2.seconds
import io.spine.server.BoundedContextBuilder
import io.spine.server.integration.ThirdPartyContext
import io.spine.testing.core.given.GivenUserId
import io.spine.testing.server.blackbox.ContextAwareTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("`UserSession` should")
internal class UserSessionSpec : ContextAwareTest() {

    private val auth = PredefinedGitHubAuthenticationResponses()
    private val users = PredefinedGitHubUsersResponses()

    override fun contextBuilder(): BoundedContextBuilder = newSessionsContext(auth, users)

    @AfterEach
    internal fun reset() {
        auth.reset()
        users.reset()
    }

    @Nested internal inner class
    `Handle 'LogUserIn' command, and` {

        private lateinit var id: SessionId

        @BeforeEach
        internal fun sendCommand() {
            id = SessionId::class.generate()
            val command = LogUserIn::class.withSession(id)
            context().receivesCommand(command)
        }

        @Test
        internal fun `emit 'UserCodeReceived' event`() {
            val expected = expectedUserCodeReceivedEvent(id)
            context().assertEvent(expected)
        }

        @Test
        internal fun `set device code in 'UserSession' entity`() {
            val expected = expectedUserSessionWithDeviceCode(id)
            context().assertState(id, expected)
        }
    }

    @Nested internal inner class
    `Handle 'VerifyUserLoginToGitHub' command, and` {

        private lateinit var id: SessionId

        @BeforeEach
        internal fun sendLogUserInCommand() {
            id = SessionId::class.generate()
            val command = LogUserIn::class.withSession(id)
            context().receivesCommand(command)
        }

        @Test
        internal fun `emit 'UserIsNotLoggedIntoGitHub' if user code has not been entered`() {
            users.username = id.username
            sendVerificationCommand()
            val expected = UserIsNotLoggedIntoGitHub::class.withSession(id)
            context().assertEvent(expected)
            context().assertEvents()
                .withType(UserLoggedIn::class.java)
                .hasSize(0)
        }

        @Test
        internal fun `emit 'UserLoggedIn' if user code has been entered`() {
            auth.enterUserCode()
            users.username = id.username
            sendVerificationCommand()
            val expected = expectedUserLoggedInEvent(id)
            context().assertEvents()
                .withType(UserIsNotLoggedIntoGitHub::class.java)
                .hasSize(0)
            val subject = context().assertEvents()
                .withType(UserLoggedIn::class.java)
            subject.hasSize(1)
            val actual = subject.actual()
                .map { AnyPacker.unpack(it.message, UserLoggedIn::class.java) }[0]
            expected.id shouldBe actual.id
            expected.token shouldBe expected.token
            val expirationDiff = between(expected.whenTokenExpires, actual.whenTokenExpires)
            expirationDiff.seconds shouldBe 0L
        }

        @Test
        internal fun `set refresh token in 'UserSession' entity if user code has been entered`() {
            auth.enterUserCode()
            users.username = id.username
            sendVerificationCommand()
            val expected = expectedUserSessionWithRefreshToken(id)
            context().assertState(id, expected)
        }

        @Test
        internal fun `reject if the user completes authentication from another account`() {
            auth.enterUserCode()
            users.username = Username::class.of("illegal")
            sendVerificationCommand()
            val expected = UsernameMismatch::class.with(id, id.username, users.username)
            context().assertEvent(expected)
        }

        @Test
        internal fun `reject if the user is not a member of the permitted organizations`() {
            auth.enterUserCode()
            users.username = id.username
            users.isMemberOfPermittedOrganizations = false
            sendVerificationCommand()
            val expected = NotMemberOfPermittedOrgs::class.with(id)
            context().assertEvent(expected)
        }

        private fun sendVerificationCommand() {
            val command = VerifyUserLoginToGitHub::class.withSession(id)
            context().receivesCommand(command)
        }
    }

    @Nested internal inner class
    `Handle 'UpdateToken' command, and` {

        private lateinit var id: SessionId
        private lateinit var whenRequested: Timestamp

        @BeforeEach
        internal fun sendUpdateTokenCommand() {
            id = SessionId::class.generate()
            logIn(id)
            whenRequested = currentTime()
            val command = UpdateToken::class.with(id, whenRequested)
            context().receivesCommand(command)
        }

        @Test
        internal fun `emit 'TokenUpdated' event`() {
            val expected = expectedTokenUpdatedEvent(id)
            val subject = context().assertEvents()
                .withType(TokenUpdated::class.java)
            subject.hasSize(1)
            val actual = subject.actual()
                .map { AnyPacker.unpack(it.message, TokenUpdated::class.java) }[0]
            expected.id shouldBe actual.id
            expected.token shouldBe expected.token
            val expirationDiff = between(expected.whenTokenExpires, actual.whenTokenExpires)
            expirationDiff.seconds shouldBe 0L
        }

        @Test
        internal fun `update refresh token in 'UserSession' entity`() {
            val expected = expectedUserSessionAfterTokenUpdate(id)
            context().assertState(id, expected)
        }
    }

    @Nested internal inner class
    `React on 'TimePassed' event, and` {

        private lateinit var session: SessionId

        @BeforeEach
        internal fun startLogin() {
            session = SessionId::class.generate()
            val command = LogUserIn::class.withSession(session)
            context().receivesCommand(command)
        }

        @Test
        internal fun `do nothing if the login deadline has not yet passed`() {
            emitTimePassedEvent(currentTime())
            assertThatNothingHappened()
        }

        @Test
        internal fun `do nothing if login deadline is not specified`() {
            auth.enterUserCode()
            users.username = session.username
            context().receivesCommand(VerifyUserLoginToGitHub::class.withSession(session))
            emitTimePassedEvent(currentTime())
            assertThatNothingHappened()
        }

        @Test
        internal fun `emit 'SessionClosed' if the login deadline has passed`() {
            val time = currentTime().add(minutes(20))
            emitTimePassedEvent(time)
            val expected = SessionClosed::class.with(session)
            context().assertEvent(expected)
        }

        @Test
        internal fun `mark state as deleted if the login deadline has passed`() {
            val time = currentTime().add(minutes(20))
            emitTimePassedEvent(time)
            context().assertEntity(session, UserSessionProcess::class.java)
                .deletedFlag()
                .isTrue()
        }

        @Test
        internal fun `emit 'SessionExpired' event if session has expired`() {
            finishLogin()
            val time = currentTime().add(lifetime).add(seconds(1))
            emitTimePassedEvent(time)
            val expected = SessionExpired::class.with(session)
            context().assertEvent(expected)
        }

        @Test
        internal fun `mark state as deleted if session has expired`() {
            finishLogin()
            val time = currentTime().add(lifetime).add(seconds(1))
            emitTimePassedEvent(time)
            context().assertEntity(session, UserSessionProcess::class.java)
                .deletedFlag()
                .isTrue()
        }

        @Test
        internal fun `do nothing if session has not expired`() {
            finishLogin()
            val time = currentTime().add(lifetime).add(seconds(-1))
            emitTimePassedEvent(time)
            assertThatNothingHappened()
        }

        private fun finishLogin() {
            auth.enterUserCode()
            users.username = session.username
            context().receivesCommand(VerifyUserLoginToGitHub::class.withSession(session))
        }

        private fun assertThatNothingHappened() {
            context().assertEvents()
                .withType(SessionClosed::class.java)
                .hasSize(0)
            context().assertEvents()
                .withType(SessionExpired::class.java)
                .hasSize(0)
            context().assertEntity(session, UserSessionProcess::class.java)
                .deletedFlag()
                .isFalse()
        }
    }

    @Nested internal inner class
    `Handle 'LogUserOut' command, and` {

        private lateinit var id: SessionId

        @BeforeEach
        internal fun sendCommand() {
            id = SessionId::class.generate()
            logIn(id)
            context().receivesCommand(LogUserOut::class.withSession(id))
        }

        @Test
        internal fun `emit 'UserLoggedOut' event`() {
            val expected = UserLoggedOut::class.with(id)
            context().assertEvent(expected)
        }

        @Test
        internal fun `delete 'UserSession' entity`() {
            context().assertEntity(id, UserSessionProcess::class.java)
                .deletedFlag()
                .isTrue()
        }
    }

    @Nested internal inner class
    `Handle 'VerifySession' command, and` {

        private lateinit var id: SessionId

        @BeforeEach
        internal fun init() {
            id = SessionId::class.generate()
        }

        @Test
        internal fun `emit 'SessionVerified' if active session verifies`() {
            logIn(id)
            val command = VerifySession::class.with(id)
            context().receivesCommand(command)
            val expired = SessionVerified::class.with(id)
            context().assertEvent(expired)
        }

        @Test
        internal fun `emit 'SessionVerificationFailed' if non-existent session is checked`() {
            assertThatVerificationFailed()
        }

        @Test
        internal fun `emit 'SessionVerificationFailed' if session has expired`() {
            logIn(id)
            emitTimePassedEvent(currentTime().add(lifetime).add(seconds(1)))
            assertThatVerificationFailed()
        }

        @Test
        internal fun `emit 'SessionVerificationFailed' if login process has not completed`() {
            val command = LogUserIn::class.withSession(id)
            context().receivesCommand(command)
            assertThatVerificationFailed()
        }

        @Test
        internal fun `emit 'SessionVerificationFailed' if session was closed`() {
            val command = LogUserIn::class.withSession(id)
            context().receivesCommand(command)
            emitTimePassedEvent(currentTime().add(maxLoginTime).add(seconds(1)))
            assertThatVerificationFailed()
        }

        private fun assertThatVerificationFailed() {
            val command = VerifySession::class.with(id)
            context().receivesCommand(command)
            val expired = SessionVerificationFailed::class.with(id)
            context().assertEvent(expired)
        }
    }

    @Test
    internal fun `support simultaneous sessions`() {
        val firstSession = SessionId::class.generate()
        val secondSession = SessionId::class.of(firstSession.username)
        logIn(firstSession)
        logIn(secondSession)

        val firstExpected = UserSession::class.with(firstSession)
        val secondExpected = UserSession::class.with(secondSession)
        context().assertState(firstSession, firstExpected)
        context().assertState(secondSession, secondExpected)
    }

    @Test
    internal fun `create new session when user logs in again`() {
        val firstSession = SessionId::class.generate()
        val secondSession = SessionId::class.of(firstSession.username)
        logIn(firstSession)
        context().receivesCommand(LogUserOut::class.withSession(firstSession))
        logIn(secondSession)

        val secondExpected = UserSession::class.with(secondSession)
        context().assertEntity(firstSession, UserSessionProcess::class.java)
            .deletedFlag()
            .isTrue()
        context().assertState(secondSession, secondExpected)
    }

    private fun logIn(id: SessionId) {
        context().receivesCommand(LogUserIn::class.withSession(id))
        auth.enterUserCode()
        users.username = id.username
        context().receivesCommand(VerifyUserLoginToGitHub::class.withSession(id))
    }

    private fun emitTimePassedEvent(time: Timestamp) {
        val clockContext = ThirdPartyContext.singleTenant("Clock")
        val event = TimePassed::class.buildBy(time)
        val actor = GivenUserId.generated()
        clockContext.emittedEvent(event, actor)
    }
}
