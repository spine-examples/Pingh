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

package io.spine.examples.pingh.mentions

import com.google.protobuf.Timestamp
import io.spine.base.Time.currentTime
import io.spine.examples.pingh.github.PersonalAccessToken
import io.spine.examples.pingh.github.Username
import io.spine.examples.pingh.github.buildBy
import io.spine.examples.pingh.mentions.command.UpdateMentionsFromGitHub
import io.spine.examples.pingh.mentions.event.GitHubTokenUpdated
import io.spine.examples.pingh.mentions.event.MentionsUpdateFromGitHubRequested
import io.spine.examples.pingh.mentions.given.GitHubClientSpecService
import io.spine.examples.pingh.mentions.given.gitHubClientBy
import io.spine.examples.pingh.mentions.given.gitHubClientIdBy
import io.spine.examples.pingh.mentions.rejection.GithubClientRejections.MentionsUpdateIsAlreadyInProgress
import io.spine.examples.pingh.sessions.SessionId
import io.spine.examples.pingh.sessions.event.UserLoggedIn
import io.spine.server.BoundedContextBuilder
import io.spine.testing.TestValues.randomString
import io.spine.testing.server.blackbox.BlackBoxContext
import io.spine.testing.server.blackbox.ContextAwareTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("GitHub Client should")
public class GitHubClientSpec : ContextAwareTest() {

    private lateinit var sessionContext: BlackBoxContext
    private lateinit var gitHubClientId: GitHubClientId
    private lateinit var token: PersonalAccessToken

    protected override fun contextBuilder(): BoundedContextBuilder =
        newBuilder(GitHubClientSpecService())

    /**
     * Creates the [BlackBoxContext] of the Sessions bounded context,
     * and further emits of the [UserLoggedIn] event in the Sessions context.
     */
    @BeforeEach
    public fun prepareSessionsContextAndEmitEvent() {
        sessionContext = BlackBoxContext
            .from(io.spine.examples.pingh.sessions.newBuilder())
        val username = Username.newBuilder()
            .buildBy(randomString())
        gitHubClientId = gitHubClientIdBy(username)
        emitUserLoggedInEventInSessionsContext()
    }

    /**
     * Creates a new token and emits the [UserLoggedIn] event
     * in the Sessions bounded context.
     */
    private fun emitUserLoggedInEventInSessionsContext() {
        token = PersonalAccessToken.newBuilder()
            .buildBy(randomString())
        val userLoggedIn = UserLoggedIn.newBuilder()
            .setId(
                SessionId.newBuilder()
                    .setUsername(gitHubClientId.username)
                    .setWhenCreated(currentTime())
                    .vBuild()
            )
            .setToken(token)
            .vBuild()
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
            val expected = GitHubTokenUpdated.newBuilder()
                .setId(gitHubClientId)
                .setToken(token)
                .vBuild()
            context().assertEvent(expected)
        }

        @Test
        public fun `create 'GitHubClient' state`() {
            val expected = gitHubClientBy(gitHubClientId, token)
            context().assertState(gitHubClientId, expected)
        }

        @Test
        public fun `update token in existing 'GitHubClient' entity`() {
            emitUserLoggedInEventInSessionsContext()
            val expected = gitHubClientBy(gitHubClientId, token)
            context().assertState(gitHubClientId, expected)
        }
    }

    @Nested
    public inner class `handle 'UpdateMentionsFromGitHub' command, and` {

        private fun sendUpdateMentionsFromGitHubCommand() {
            val command = UpdateMentionsFromGitHub.newBuilder()
                .setId(gitHubClientId)
                .setWhenRequested(currentTime())
                .vBuild()
            context().receivesCommand(command)
        }

        @Test
        public fun `emits 'MentionsUpdateFromGitHubRequested' event if there is no active update process at this time`() {
            sendUpdateMentionsFromGitHubCommand()
            val expected = MentionsUpdateFromGitHubRequested.newBuilder()
                .setId(gitHubClientId)
                .vBuild()
            context().assertEvent(expected)
        }

        /**
         * Checks that the value of the `when_started` field is different from the default,
         * i.e. has been set to some value.
         *
         * [MentionsUpdateFromGitHubRequested] command is sent from another thread,
         * and the main thread checks the state of the entity while performing the update.
         */
        @Test
        public fun `set the start time of the updating operation in 'GitHubClient' entity`() {
            val otherClientTread = Thread {
                sendUpdateMentionsFromGitHubCommand()
            }
            val gitHubClientWithDefaultWhenStartedField = with(GitHubClient.newBuilder()) {
                whenStarted = Timestamp.getDefaultInstance()
                // Building the message partially to include
                // only the tested fields.
                buildPartial()
            }
            try {
                otherClientTread.start()
                context().assertState(gitHubClientId, GitHubClient::class.java)
                    .ignoringFields(listOf(1, 2))
                    .isNotEqualTo(gitHubClientWithDefaultWhenStartedField)
            } finally {
                otherClientTread.join()
            }
        }

        /**
         * The update request is sent from another thread.
         *
         * [MentionsUpdateFromGitHubRequested] command that is sent from another thread
         * is successfully accepted, after which the command from the main thread is received.
         * The command from the main thread must be rejected, because
         * the process started in another thread is not yet complete.
         */
        @Test
        public fun `reject if the update process is already in progress at this time`() {
            val otherClientTread = Thread {
                sendUpdateMentionsFromGitHubCommand()
            }
            val expectedRejection = MentionsUpdateIsAlreadyInProgress.newBuilder()
                .setId(gitHubClientId)
                .vBuild()
            try {
                otherClientTread.start()
                sendUpdateMentionsFromGitHubCommand()
                context().assertEvent(expectedRejection)
            } finally {
                otherClientTread.join()
            }
        }
    }
}
