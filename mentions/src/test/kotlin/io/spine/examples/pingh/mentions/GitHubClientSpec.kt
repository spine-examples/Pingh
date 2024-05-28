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

import io.spine.examples.pingh.github.PersonalAccessToken
import io.spine.examples.pingh.github.Username
import io.spine.examples.pingh.github.buildBy
import io.spine.examples.pingh.mentions.command.UpdateMentionsFromGitHub
import io.spine.examples.pingh.mentions.event.GitHubTokenUpdated
import io.spine.examples.pingh.mentions.event.MentionsUpdateFromGitHubRequested
import io.spine.examples.pingh.mentions.given.PredefinedGitHubResponses
import io.spine.examples.pingh.mentions.given.buildBy
import io.spine.examples.pingh.mentions.given.buildWithDefaultWhenStartedField
import io.spine.examples.pingh.mentions.rejection.GithubClientRejections.MentionsUpdateIsAlreadyInProgress
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
        newBuilder(PredefinedGitHubResponses())

    /**
     * Creates the [BlackBoxContext] of the Sessions bounded context,
     * and further emits of the [UserLoggedIn] event in the Sessions context.
     */
    @BeforeEach
    public fun prepareSessionsContextAndEmitEvent() {
        sessionContext = BlackBoxContext
            .from(io.spine.examples.pingh.sessions.newBuilder())
        val username = Username::class.buildBy(randomString())
        gitHubClientId = GitHubClientId::class.buildBy(username)
        emitUserLoggedInEventInSessionsContext()
    }

    /**
     * Creates a new token and emits the [UserLoggedIn] event
     * in the Sessions bounded context.
     */
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
        public fun `init the 'GitHubClient' state and set the received token`() {
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
        public fun `emits 'MentionsUpdateFromGitHubRequested' event if there is no active update process at this time`() {
            val command = UpdateMentionsFromGitHub::class.buildBy(gitHubClientId)
            context().receivesCommand(command)
            val expected = MentionsUpdateFromGitHubRequested::class.buildBy(gitHubClientId)
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
        public fun `set the start time of the updating operation`() {
            val otherClientTread = Thread {
                val command = UpdateMentionsFromGitHub::class.buildBy(gitHubClientId)
                context().receivesCommand(command)
            }
            val gitHubClientWithDefaultWhenStartedField =
                GitHubClient::class.buildWithDefaultWhenStartedField()
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
         * Checks if the [UpdateMentionsFromGitHub] command is rejected
         * if the previous process is not completed.
         *
         * Creates a second thread from which the [UpdateMentionsFromGitHub] command is sent.
         * It should be successfully accepted, which will start the update process.
         * After that, another [UpdateMentionsFromGitHub] command is sent from the main thread,
         * but it should be rejected because the update process started from
         * the other thread has not completed.
         */
        @Test
        public fun `reject if the update process is already in progress at this time`() {
            val otherClientTread = Thread {
                val firstCommand = UpdateMentionsFromGitHub::class.buildBy(gitHubClientId)
                context().receivesCommand(firstCommand)
            }
            val secondCommand = UpdateMentionsFromGitHub::class.buildBy(gitHubClientId)
            val expectedRejection = MentionsUpdateIsAlreadyInProgress::class.buildBy(gitHubClientId)
            try {
                otherClientTread.start()
                context().receivesCommand(secondCommand)
                context().assertEvent(expectedRejection)
            } finally {
                otherClientTread.join()
            }
        }
    }
}
