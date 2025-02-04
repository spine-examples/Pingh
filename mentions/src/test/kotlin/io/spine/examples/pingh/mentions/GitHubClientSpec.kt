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
import com.google.protobuf.util.Timestamps
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import io.spine.base.Time.currentTime
import io.spine.core.UserId
import io.spine.examples.pingh.clock.buildBy
import io.spine.examples.pingh.clock.event.TimePassed
import io.spine.examples.pingh.github.Mention
import io.spine.examples.pingh.github.PersonalAccessToken
import io.spine.examples.pingh.github.Team
import io.spine.examples.pingh.github.User
import io.spine.examples.pingh.github.Username
import io.spine.examples.pingh.github.of
import io.spine.examples.pingh.mentions.GitHubClientProcess.Companion.limitOnFirstLaunch
import io.spine.examples.pingh.mentions.GitHubClientProcess.Companion.mentionsUpdateInterval
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
import io.spine.examples.pingh.sessions.event.TokenUpdated
import io.spine.examples.pingh.sessions.event.UserLoggedIn
import io.spine.examples.pingh.sessions.newSessionsContext
import io.spine.examples.pingh.sessions.of
import io.spine.examples.pingh.sessions.with
import io.spine.examples.pingh.testing.mentions.given.PredefinedGitHubSearchResponses
import io.spine.examples.pingh.testing.sessions.given.PredefinedGitHubAuthenticationResponses
import io.spine.examples.pingh.testing.sessions.given.PredefinedGitHubUsersResponses
import io.spine.net.Url
import io.spine.protobuf.Durations2.minutes
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
    private val users = PredefinedGitHubUsersResponses()
    private lateinit var sessionContext: BlackBoxContext
    private lateinit var gitHubClientId: GitHubClientId
    private lateinit var sessionId: SessionId
    private lateinit var token: PersonalAccessToken

    override fun contextBuilder(): BoundedContextBuilder =
        newMentionsContext(search, users)

    @BeforeEach
    internal fun prepareSessionsContextAndEmitEvent() {
        search.reset()
        users.reset()
        sessionContext = BlackBoxContext
            .from(
                newSessionsContext(
                    PredefinedGitHubAuthenticationResponses(),
                    PredefinedGitHubUsersResponses()
                )
            )
        val username = Username::class.of(randomString())
        gitHubClientId = GitHubClientId::class.of(username)
        emitUserLoggedInEvent()
    }

    /**
     * Note that after emitting `UserLoggedIn` event,
     * the `UpdateMentionsFromGitHub` command is sent.
     */
    private fun emitUserLoggedInEvent() {
        token = PersonalAccessToken::class.of(randomString())
        sessionId = SessionId::class.of(gitHubClientId.username)
        val userLoggedIn = UserLoggedIn::class.with(sessionId, token, Timestamps.MAX_VALUE)
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
            emitUserLoggedInEvent()
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
            val event = TokenUpdated::class.with(sessionId, newToken, currentTime())
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
            emitUserLoggedInEvent()
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
            val expected = MentionsUpdateFromGitHubCompleted::class.buildBy(gitHubClientId)
            context().assertEvent(expected)
        }

        @Test
        internal fun `emit 'RequestMentionsFromGitHubFailed' event if request to GitHub failed`() {
            val responseStatusCode = HttpStatusCode.ServiceUnavailable
            search.setResponseStatusCode(responseStatusCode)
            gitHubClientId = GitHubClientId::class.of(
                Username::class.of(randomString())
            )
            emitUserLoggedInEvent()
            val expected = RequestMentionsFromGitHubFailed::class.with(
                gitHubClientId,
                responseStatusCode.value
            )
            context().assertEvents()
                .withType(UserMentioned::class.java)
                .actual()
                .map { it.message.unpack<UserMentioned>() }
                .filter { it.id.equals(gitHubClientId) }
                .shouldBeEmpty()
            context().assertEvents()
                .withType(MentionsUpdateFromGitHubCompleted::class.java)
                .actual()
                .map { it.message.unpack<MentionsUpdateFromGitHubCompleted>() }
                .filter { it.id.equals(gitHubClientId) }
                .shouldBeEmpty()
            context().assertEvent(expected)
            search.setDefaultResponseStatusCode()
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
            .hasSize(3)
    }

    @Test
    internal fun `do not duplicate mentions made on the same item`() {
        gitHubClientId = GitHubClientId::class.of(Username::class.of(randomString()))
        val team = Team::class.generate()
        val userMention = Mention::class.generate { user = gitHubClientId.username }
        val teamMention = userMention.toBuilder()
            .clearUser()
            .setTeam(team)
            .vBuild()
        search.injectUserMention(userMention)
        search.injectTeamMention(teamMention)
        val mentionId = MentionId::class.of(userMention.id, gitHubClientId.username)
        emitUserLoggedInEvent()
        userMentioned(mentionId) shouldHaveSize 1
    }

    @Test
    internal fun `ignore self-mention`() {
        gitHubClientId = GitHubClientId::class.of(Username::class.of(randomString()))
        val mention = Mention::class.generate {
            author = User::class.of(gitHubClientId.username, Url::class.generate())
            user = gitHubClientId.username
        }
        search.injectUserMention(mention)
        val mentionId = MentionId::class.of(mention.id, gitHubClientId.username)
        emitUserLoggedInEvent()
        userMentioned(mentionId).shouldBeEmpty()
    }

    @Test
    internal fun `ignore team mention if the user made it themselves`() {
        gitHubClientId = GitHubClientId::class.of(Username::class.of(randomString()))
        val team = Team::class.generate()
        val mention = Mention::class.generate {
            author = User::class.of(gitHubClientId.username, Url::class.generate())
            this.team = team
        }
        search.injectTeamMention(mention)
        val mentionId = MentionId::class.of(mention.id, gitHubClientId.username)
        emitUserLoggedInEvent()
        userMentioned(mentionId).shouldBeEmpty()
    }

    private fun userMentioned(id: MentionId): List<UserMentioned> =
        context().assertEvents()
            .withType(UserMentioned::class.java)
            .actual()
            .map { it.message.unpack<UserMentioned>() }
            .filter { it.id.equals(id) }

    @Test
    internal fun `limit number of mentions received on the first request`() {
        gitHubClientId = GitHubClientId::class.of(Username::class.of(randomString()))
        var time = currentTime()
        val mentions = mutableListOf<Mention>()
        for (i in 1..(limitOnFirstLaunch * 2)) {
            val mention = Mention::class.generate {
                user = gitHubClientId.username
                whenMentioned = time
            }
            search.injectUserMention(mention)
            mentions.add(mention)
            time = time.add(minutes(1))
        }
        val expected = mentions
            .sortedByDescending { Timestamps.toNanos(it.whenMentioned) }
            .map { mention -> UserMentioned::class.buildBy(mention, gitHubClientId.username) }
            .take(limitOnFirstLaunch)
            .toSet()
        emitUserLoggedInEvent()
        val actual = context().assertEvents()
            .withType(UserMentioned::class.java)
            .actual()
            .map { it.message.unpack<UserMentioned>() }
            .filter { it.id.user.equals(gitHubClientId.username) }
            .toSet()
        actual shouldHaveSize limitOnFirstLaunch
        actual shouldBe expected
    }
}
