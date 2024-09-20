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

package io.spine.examples.pingh.mentions

import com.google.protobuf.Timestamp
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import io.spine.base.Time.currentTime
import io.spine.core.UserId
import io.spine.examples.pingh.clock.buildBy
import io.spine.examples.pingh.clock.event.TimePassed
import io.spine.examples.pingh.github.PersonalAccessToken
import io.spine.examples.pingh.github.Username
import io.spine.examples.pingh.github.of
import io.spine.examples.pingh.mentions.command.UpdateMentionsFromGitHub
import io.spine.examples.pingh.mentions.event.GitHubTokenUpdated
import io.spine.examples.pingh.mentions.event.MentionsUpdateFromGitHubCompleted
import io.spine.examples.pingh.mentions.event.MentionsUpdateFromGitHubRequested
import io.spine.examples.pingh.mentions.event.RequestMentionsFromGitHubFailed
import io.spine.examples.pingh.mentions.event.UserMentioned
import io.spine.examples.pingh.mentions.given.buildBy
import io.spine.examples.pingh.mentions.given.buildWithDefaultWhenStartedField
import io.spine.examples.pingh.mentions.given.expectedUserMentionedSet
import io.spine.examples.pingh.mentions.given.generate
import io.spine.examples.pingh.mentions.rejection.GithubClientRejections.MentionsUpdateIsAlreadyInProgress
import io.spine.examples.pingh.sessions.SessionId
import io.spine.examples.pingh.sessions.buildBy
import io.spine.examples.pingh.sessions.event.TokenRefreshed
import io.spine.examples.pingh.sessions.event.UserLoggedIn
import io.spine.examples.pingh.sessions.newSessionsContext
import io.spine.examples.pingh.sessions.of
import io.spine.examples.pingh.sessions.with
import io.spine.examples.pingh.testing.mentions.given.PredefinedGitHubSearchResponses
import io.spine.examples.pingh.testing.sessions.given.PredefinedGitHubAuthenticationResponses
import io.spine.examples.pingh.testing.sessions.given.PredefinedGitHubUsersResponses
import io.spine.protobuf.Durations2.seconds
import io.spine.server.BoundedContextBuilder
import io.spine.server.integration.ThirdPartyContext
import io.spine.testing.TestValues.randomString
import io.spine.testing.server.blackbox.BlackBoxContext
import io.spine.testing.server.blackbox.ContextAwareTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("`GitHubClient` should")
internal class GitHubClientSpec : ContextAwareTest() {

    private val search = PredefinedGitHubSearchResponses()
    private lateinit var sessionContext: BlackBoxContext
    private lateinit var gitHubClientId: GitHubClientId
    private lateinit var sessionId: SessionId
    private lateinit var token: PersonalAccessToken

    override fun contextBuilder(): BoundedContextBuilder =
        newMentionsContext(search)

    @BeforeEach
    internal fun prepareSessionsContextAndEmitEvent() {
        search.reset()
        sessionContext = BlackBoxContext
            .from(
                newSessionsContext(
                    PredefinedGitHubAuthenticationResponses(),
                    PredefinedGitHubUsersResponses()
                )
            )
        val username = Username::class.of(randomString())
        gitHubClientId = GitHubClientId::class.of(username)
        emitUserLoggedInEventInSessionsContext()
    }

    private fun emitUserLoggedInEventInSessionsContext() {
        token = PersonalAccessToken::class.of(randomString())
        sessionId = SessionId::class.of(gitHubClientId.username)
        val userLoggedIn = UserLoggedIn::class.buildBy(sessionId, token)
        sessionContext.receivesEvent(userLoggedIn)
    }

    @AfterEach
    internal fun closeSessionsContext() {
        sessionContext.close()
    }

    @Nested internal inner class
    `React on 'UserLoggedIn' event in Sessions bounded context, and` {

        @Test
        internal fun `emit 'GitHubTokenUpdated' event`() {
            val expected = GitHubTokenUpdated::class.buildBy(gitHubClientId, token)
            context().assertEvent(expected)
        }

        @Test
        internal fun `init 'GitHubClient' state and set the received token`() {
            val expected = GitHubClient::class.buildBy(gitHubClientId, token)
            context().assertState(gitHubClientId, expected)
        }

        @Test
        internal fun `update token in existing 'GitHubClient' entity`() {
            emitUserLoggedInEventInSessionsContext()
            val expected = GitHubClient::class.buildBy(gitHubClientId, token)
            context().assertState(gitHubClientId, expected)
        }
    }

    @Nested internal inner class
    `React on 'TokenRefreshed' event in Sessions bounded context, and` {

        private lateinit var newToken: PersonalAccessToken

        @BeforeEach
        internal fun emitTokenRefreshedEvent() {
            newToken = PersonalAccessToken::class.of(randomString())
            val event = TokenRefreshed::class.with(sessionId, newToken, currentTime())
            sessionContext.receivesEvent(event)
        }

        @Test
        internal fun `emit 'GitHubTokenUpdated' event`() {
            val expected = GitHubTokenUpdated::class.buildBy(gitHubClientId, newToken)
            val eventSubject = context().assertEvents()
                .withType(GitHubTokenUpdated::class.java)
            eventSubject.hasSize(2)
            eventSubject.message(1)
                .isEqualTo(expected)
        }

        @Test
        internal fun `update token in 'GitHubClient' entity`() {
            val expected = GitHubClient::class.buildBy(gitHubClientId, newToken)
            context().assertState(gitHubClientId, expected)
        }
    }

    @Nested internal inner class
    `React on 'TimePassed' event, and` {

        @Test
        internal fun `send update command if no updates have been made yet`() {
            val time = currentTime()
            emitTimePassedEvent(time)
            val expected = UpdateMentionsFromGitHub::class.buildBy(gitHubClientId, time)
            val commandSubject = context().assertCommands()
                .withType(UpdateMentionsFromGitHub::class.java)
            commandSubject.hasSize(1)
            commandSubject.message(0)
                .isEqualTo(expected)
        }

        @Test
        internal fun `send update command if required time since the last update has passed`() {
            val firstRequestTime = currentTime()
            val lastRequestTime = firstRequestTime.add(mentionsUpdateInterval)
            emitTimePassedEvent(firstRequestTime)
            emitTimePassedEvent(lastRequestTime)
            val expected = UpdateMentionsFromGitHub::class.buildBy(gitHubClientId, lastRequestTime)
            val commandSubject = context().assertCommands()
                .withType(UpdateMentionsFromGitHub::class.java)
            commandSubject.hasSize(2)
            commandSubject.message(1)
                .isEqualTo(expected)
        }

        @Test
        internal fun `do nothing if the required time between updates has not passed`() {
            val time = currentTime()
            emitTimePassedEvent(time)
            emitTimePassedEvent(time.add(seconds(1)))
            context().assertCommands()
                .withType(UpdateMentionsFromGitHub::class.java)
                .hasSize(1)
        }

        private fun emitTimePassedEvent(time: Timestamp) {
            val clockContext = ThirdPartyContext.singleTenant("Clock")
            val event = TimePassed::class.buildBy(time)
            val actor = UserId::class.generate()
            clockContext.emittedEvent(event, actor)
        }
    }

    @Nested internal inner class
    `Handle 'UpdateMentionsFromGitHub' command, and` {

        @Test
        internal fun `emit 'MentionsUpdateFromGitHubRequested' event if update process started`() {
            val command = UpdateMentionsFromGitHub::class.buildBy(gitHubClientId)
            context().receivesCommand(command)
            val expected = MentionsUpdateFromGitHubRequested::class.buildBy(gitHubClientId)
            context().assertEvent(expected)
        }

        @Test
        internal fun `set the start time of the update operation`() {
            val otherClientThread = Thread {
                val command = UpdateMentionsFromGitHub::class.buildBy(gitHubClientId)
                context().receivesCommand(command)
            }
            val gitHubClientWithDefaultWhenStartedField =
                GitHubClient::class.buildWithDefaultWhenStartedField()
            try {
                otherClientThread.start()
                context().assertState(gitHubClientId, GitHubClient::class.java)
                    .ignoringFields(listOf(1, 2))
                    .isNotEqualTo(gitHubClientWithDefaultWhenStartedField)
            } finally {
                otherClientThread.join()
            }
        }

        @Test
        internal fun `reject if the update process is already in progress at this time`() {
            search.freeze()
            val firstClientThread = Thread {
                val firstCommand = UpdateMentionsFromGitHub::class.buildBy(gitHubClientId)
                context().receivesCommand(firstCommand)
                search.unfreeze()
            }
            val secondClientThread = Thread {
                val secondCommand = UpdateMentionsFromGitHub::class.buildBy(gitHubClientId)
                context().receivesCommand(secondCommand)
                search.unfreeze()
            }
            val expectedRejection =
                MentionsUpdateIsAlreadyInProgress::class.buildBy(gitHubClientId)

            firstClientThread.start()
            secondClientThread.start()
            firstClientThread.join()
            secondClientThread.join()

            context().assertEvent(expectedRejection)
        }
    }

    @Nested internal inner class
    `React on 'MentionsUpdateFromGitHubRequested' event, and` {

        @Test
        internal fun `emit 'UserMentioned' events for each mentions fetched from GitHub`() {
            emitMentionsUpdateFromGitHubRequestedEvent()
            val expectedUserMentionedSet = expectedUserMentionedSet(gitHubClientId.username)
            val eventSubject = context().assertEvents()
                .withType(UserMentioned::class.java)
            eventSubject.hasSize(expectedUserMentionedSet.size)
            val actualUserMentionedSet = eventSubject
                .actual()
                .map { it.message.unpack<UserMentioned>() }
                .toSet()
            actualUserMentionedSet shouldBe expectedUserMentionedSet
        }

        @Test
        internal fun `emit 'MentionsUpdateFromGitHubCompleted' event`() {
            emitMentionsUpdateFromGitHubRequestedEvent()
            val expected = MentionsUpdateFromGitHubCompleted::class.buildBy(gitHubClientId)
            context().assertEvent(expected)
        }

        @Test
        internal fun `emit 'RequestMentionsFromGitHubFailed' event if request to GitHub failed`() {
            val responseStatusCode = HttpStatusCode.ServiceUnavailable
            search.setResponseStatusCode(responseStatusCode)
            emitMentionsUpdateFromGitHubRequestedEvent()
            val expected = RequestMentionsFromGitHubFailed::class.buildBy(
                gitHubClientId,
                responseStatusCode.value
            )
            context().assertEvents()
                .withType(UserMentioned::class.java)
                .hasSize(0)
            context().assertEvents()
                .withType(MentionsUpdateFromGitHubCompleted::class.java)
                .hasSize(0)
            context().assertEvent(expected)
            search.setDefaultResponseStatusCode()
        }

        private fun emitMentionsUpdateFromGitHubRequestedEvent() {
            val event = MentionsUpdateFromGitHubRequested::class.buildBy(gitHubClientId)
            context().receivesEvent(event)
        }
    }

    @Test
    internal fun `reset process start time after completing the update process`() {
        val command = UpdateMentionsFromGitHub::class.buildBy(gitHubClientId)
        context().receivesCommand(command)
        val expected = GitHubClient::class.buildBy(gitHubClientId, token)
        context().assertState(gitHubClientId, expected)
    }

    @Test
    internal fun `remember the time of last successful update`() {
        val whenRequested = currentTime()
        val command = UpdateMentionsFromGitHub::class.buildBy(gitHubClientId, whenRequested)
        context().receivesCommand(command)
        val expected = GitHubClient::class.buildBy(gitHubClientId, token, whenRequested)
        context().assertState(gitHubClientId, expected)
    }

    @Test
    internal fun `allow new update process after the previous one is completed`() {
        val firstCommand = UpdateMentionsFromGitHub::class.buildBy(gitHubClientId)
        context().receivesCommand(firstCommand)
        val secondCommand = UpdateMentionsFromGitHub::class.buildBy(gitHubClientId)
        context().receivesCommand(secondCommand)
        context().assertEvents()
            .withType(MentionsUpdateFromGitHubRequested::class.java)
            .hasSize(2)
    }
}
