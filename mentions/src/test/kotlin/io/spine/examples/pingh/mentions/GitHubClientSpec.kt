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

import io.spine.base.Time.currentTime
import io.spine.examples.pingh.github.PersonalAccessToken
import io.spine.examples.pingh.github.Username
import io.spine.examples.pingh.mentions.event.GitHubTokenUpdated
import io.spine.examples.pingh.mentions.given.gitHubClientBy
import io.spine.examples.pingh.mentions.given.gitHubClientIdBy
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

    protected override fun contextBuilder(): BoundedContextBuilder =
        io.spine.examples.pingh.mentions.given.newBuilder()

    @Nested
    public inner class `react on 'UserLoggedIn' event in Sessions bounded context, and` {

        private lateinit var sessionContext: BlackBoxContext
        private lateinit var username: Username
        private lateinit var token: PersonalAccessToken

        /**
         * Creates the [BlackBoxContext] of the Sessions bounded context,
         * and further emits of the [UserLoggedIn] event in the Sessions context.
         */
        @BeforeEach
        public fun prepareSessionsContextAndEmitEvent() {
            sessionContext = BlackBoxContext
                .from(io.spine.examples.pingh.sessions.newBuilder())
            username = with(Username.newBuilder()) {
                value = randomString()
                vBuild()
            }
            emitUserLoggedInEventInSessionsContext()
        }

        /**
         * Creates a new token and emits the [UserLoggedIn] event
         * in the Sessions bounded context.
         */
        private fun emitUserLoggedInEventInSessionsContext() {
            token = with(PersonalAccessToken.newBuilder()) {
                value = randomString()
                vBuild()
            }
            val userLoggedIn = UserLoggedIn.newBuilder()
                .setId(
                    SessionId.newBuilder()
                        .setUsername(username)
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

        @Test
        public fun `emit 'GitHubTokenUpdated' event`() {
            val id = gitHubClientIdBy(username)
            val expected = GitHubTokenUpdated.newBuilder()
                .setId(id)
                .setToken(token)
                .vBuild()
            context().assertEvent(expected)
        }

        @Test
        public fun `create 'GitHubClient' state`() {
            val id = gitHubClientIdBy(username)
            val expected = gitHubClientBy(id, token)
            context().assertState(id, expected)
        }

        @Test
        public fun `update token in existing 'GitHubClient' entity`() {
            val id = gitHubClientIdBy(username)
            emitUserLoggedInEventInSessionsContext()
            val expected = gitHubClientBy(id, token)
            context().assertState(id, expected)
        }
    }

    @Nested
    public inner class `handle 'UpdateMentionsFromGitHub' command, and` {
        // TODO:2024-05-21:mykyta.pimonov: Test it.
    }

    @Nested
    public inner class `react on 'MentionsUpdateFromGitHubRequested' event, and` {
        // TODO:2024-05-21:mykyta.pimonov: Test it.
    }
}
