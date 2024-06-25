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

import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import io.spine.base.Time.currentTime
import io.spine.examples.pingh.github.PersonalAccessToken
import io.spine.examples.pingh.github.Username
import io.spine.examples.pingh.github.buildBy
import io.spine.examples.pingh.mentions.command.UpdateMentionsFromGitHub
import io.spine.examples.pingh.mentions.event.GitHubTokenUpdated
import io.spine.examples.pingh.mentions.event.MentionsUpdateFromGitHubCompleted
import io.spine.examples.pingh.mentions.event.MentionsUpdateFromGitHubRequested
import io.spine.examples.pingh.mentions.event.RequestMentionsFromGitHubFailed
import io.spine.examples.pingh.mentions.event.UserMentioned
import io.spine.examples.pingh.mentions.given.buildBy
import io.spine.examples.pingh.mentions.given.buildWithDefaultWhenLastSuccessfulUpdateField
import io.spine.examples.pingh.mentions.given.buildWithDefaultWhenStartedField
import io.spine.examples.pingh.mentions.given.expectedUserMentionedSet
import io.spine.examples.pingh.mentions.given.inMinute
import io.spine.examples.pingh.mentions.rejection.GithubClientRejections.MentionsUpdateIsAlreadyInProgress
import io.spine.examples.pingh.sessions.event.UserLoggedIn
import io.spine.examples.pingh.sessions.newSessionsContext
import io.spine.examples.pingh.testing.mentions.given.PredefinedGitHubResponses
import io.spine.protobuf.AnyPacker
import io.spine.server.BoundedContextBuilder
import io.spine.testing.TestValues.randomString
import io.spine.testing.server.blackbox.BlackBoxContext
import io.spine.testing.server.blackbox.ContextAwareTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("`GitHubClient` should")
public class GitHubClientSpec : ContextAwareTest() {

    private val gitHubClientService = PredefinedGitHubResponses()
    private lateinit var sessionContext: BlackBoxContext
    private lateinit var gitHubClientId: GitHubClientId
    private lateinit var token: PersonalAccessToken

    protected override fun contextBuilder(): BoundedContextBuilder =
        newMentionsContext(gitHubClientService)

    @BeforeEach
    public fun prepareSessionsContextAndEmitEvent() {
        gitHubClientService.unfreeze()
        gitHubClientService.setDefaultResponseStatusCode()
        gitHubClientService.clearSuccessfulUpdateTimes()
        sessionContext = BlackBoxContext.from(newSessionsContext())
        val username = Username::class.buildBy(randomString())
        gitHubClientId = GitHubClientId::class.buildBy(username)
        emitUserLoggedInEventInSessionsContext()
    }

    private fun emitUserLoggedInEventInSessionsContext() {
        token = PersonalAccessToken::class.buildBy(randomString())
        val userLoggedIn = UserLoggedIn::class.buildBy(gitHubClientId.username, token)
        sessionContext.receivesEvent(userLoggedIn)
    }

    @AfterEach
    public fun closeSessionsContext() {
        sessionContext.close()
    }

    @Nested
    public inner class `react on 'UserLoggedIn' event in Sessions bounded context, and` {

        @Test
        public fun `emit 'GitHubTokenUpdated' event`() {
            val expected = GitHubTokenUpdated::class.buildBy(gitHubClientId, token)
            context().assertEvent(expected)
        }

        @Test
        public fun `init 'GitHubClient' state and set the received token`() {
            val expected = GitHubClient::class.buildBy(gitHubClientId, token)
            context().assertState(gitHubClientId, expected)
        }

        @Test
        public fun `update token in existing 'GitHubClient' entity`() {
            emitUserLoggedInEventInSessionsContext()
            val expected = GitHubClient::class.buildBy(gitHubClientId, token)
            context().assertState(gitHubClientId, expected)
        }
    }

    @Nested
    public inner class `handle 'UpdateMentionsFromGitHub' command, and` {

        @Test
        public fun `emit 'MentionsUpdateFromGitHubRequested' event if update process started`() {
            val command = UpdateMentionsFromGitHub::class.buildBy(gitHubClientId)
            context().receivesCommand(command)
            val expected = MentionsUpdateFromGitHubRequested::class.buildBy(gitHubClientId)
            context().assertEvent(expected)
        }

        @Test
        public fun `set the start time of the update operation`() {
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
        public fun `reject if the update process is already in progress at this time`() {
            gitHubClientService.freeze()
            val firstClientThread = Thread {
                val firstCommand = UpdateMentionsFromGitHub::class.buildBy(gitHubClientId)
                context().receivesCommand(firstCommand)
                gitHubClientService.unfreeze()
            }
            val secondClientThread = Thread {
                val secondCommand = UpdateMentionsFromGitHub::class.buildBy(gitHubClientId)
                context().receivesCommand(secondCommand)
                gitHubClientService.unfreeze()
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

    @Nested
    public inner class `react on 'MentionsUpdateFromGitHubRequested' event, and` {

        @Test
        public fun `emit 'UserMentioned' events for each mentions fetched from GitHub`() {
            emitMentionsUpdateFromGitHubRequestedEvent()
            val expectedUserMentionedSet = expectedUserMentionedSet(gitHubClientId.username)
            val eventSubject = context().assertEvents()
                .withType(UserMentioned::class.java)
            eventSubject.hasSize(expectedUserMentionedSet.size)
            val actualUserMentionedSet = eventSubject
                .actual()
                .map { event -> AnyPacker.unpack(event.message, UserMentioned::class.java) }
                .toSet()
            actualUserMentionedSet shouldBe expectedUserMentionedSet
        }

        @Test
        public fun `emit 'MentionsUpdateFromGitHubCompleted' event`() {
            emitMentionsUpdateFromGitHubRequestedEvent()
            val expected = MentionsUpdateFromGitHubCompleted::class.buildBy(gitHubClientId)
            context().assertEvent(expected)
        }

        @Test
        public fun `emit 'RequestMentionsFromGitHubFailed' event if request to GitHub failed`() {
            val responseStatusCode = HttpStatusCode.ServiceUnavailable
            gitHubClientService.setResponseStatusCode(responseStatusCode)
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
            gitHubClientService.setDefaultResponseStatusCode()
        }

        private fun emitMentionsUpdateFromGitHubRequestedEvent() {
            val event = MentionsUpdateFromGitHubRequested::class.buildBy(gitHubClientId)
            context().receivesEvent(event)
        }
    }

    @Test
    public fun `clear 'when_started' field after completing the update process`() {
        val command = UpdateMentionsFromGitHub::class.buildBy(gitHubClientId)
        context().receivesCommand(command)
        val expected = GitHubClient::class.buildBy(gitHubClientId, token)
        context().assertState(gitHubClientId, expected)
    }

    @Test
    public fun `update 'when_last_successful_update' field after completing the update process`() {
        val command = UpdateMentionsFromGitHub::class.buildBy(gitHubClientId)
        context().receivesCommand(command)
        val expected = GitHubClient::class.buildWithDefaultWhenLastSuccessfulUpdateField()
        context().assertEntity(gitHubClientId, GitHubClientProcess::class.java)
            .hasStateThat()
            .ignoringFields(1, 2, 3)
            .isNotEqualTo(expected)
    }

    @Test
    public fun `allow new update process after the previous one is completed`() {
        val firstCommand = UpdateMentionsFromGitHub::class.buildBy(gitHubClientId)
        context().receivesCommand(firstCommand)
        val secondCommand = UpdateMentionsFromGitHub::class.buildBy(gitHubClientId)
        context().receivesCommand(secondCommand)
        context().assertEvents()
            .withType(MentionsUpdateFromGitHubRequested::class.java)
            .hasSize(2)
    }

    @Test
    public fun `update the time of the last time data was updated from GitHub`() {
        val firstWhenRequested = currentTime()
        val secondWhenRequested = firstWhenRequested.inMinute()
        val firstCommand =
            UpdateMentionsFromGitHub::class.buildBy(gitHubClientId, firstWhenRequested)
        val secondCommand =
            UpdateMentionsFromGitHub::class.buildBy(gitHubClientId, secondWhenRequested)
        context()
            .receivesCommand(firstCommand)
            .receivesCommand(secondCommand)
        val expected = listOf(definePreviousWorkday(), firstWhenRequested)
        gitHubClientService.successfulUpdateTimes() shouldBe expected
    }
}
