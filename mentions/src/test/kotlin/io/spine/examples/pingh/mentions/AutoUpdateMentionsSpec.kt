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
import io.spine.base.Time.currentTime
import io.spine.core.UserId
import io.spine.examples.pingh.clock.buildBy
import io.spine.examples.pingh.clock.event.TimePassed
import io.spine.examples.pingh.mentions.command.UpdateMentionsFromGitHub
import io.spine.examples.pingh.mentions.event.AutoUpdateMentionsStarted
import io.spine.examples.pingh.mentions.event.GitHubTokenUpdated
import io.spine.examples.pingh.mentions.given.buildBy
import io.spine.examples.pingh.mentions.given.generate
import io.spine.examples.pingh.mentions.given.with
import io.spine.examples.pingh.mentions.given.withId
import io.spine.examples.pingh.sessions.event.UserLoggedIn
import io.spine.examples.pingh.sessions.newSessionsContext
import io.spine.examples.pingh.testing.mentions.given.PredefinedGitHubSearchResponses
import io.spine.examples.pingh.testing.sessions.given.PredefinedGitHubAuthenticationResponses
import io.spine.protobuf.Durations2.seconds
import io.spine.server.BoundedContextBuilder
import io.spine.server.integration.ThirdPartyContext
import io.spine.testing.server.blackbox.BlackBoxContext
import io.spine.testing.server.blackbox.ContextAwareTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("`AutoUpdateMentions` should")
internal class AutoUpdateMentionsSpec : ContextAwareTest() {

    private lateinit var id: GitHubClientId

    override fun contextBuilder(): BoundedContextBuilder =
        newMentionsContext(PredefinedGitHubSearchResponses())

    /**
     * Initializes the [GitHubClient] entity.
     *
     * The `GitHubTokenUpdated` event is [emitted][GitHubClientProcess.on].
     */
    @BeforeEach
    internal fun emitUserLoggedInEvent() {
        id = GitHubClientId::class.generate()
        val event = UserLoggedIn::class.withId(id)
        val sessionsContext = BlackBoxContext
            .from(newSessionsContext(PredefinedGitHubAuthenticationResponses()))
        sessionsContext.receivesEvent(event)
    }

    @Nested internal inner class
    `React on 'GitHubTokenUpdated' event, and` {

        @Test
        internal fun `init 'AutoUpdateMentions' entity`() {
            val expected = AutoUpdateMentions::class.with(id)
            context().assertState(id, expected)
        }

        @Test
        internal fun `emit the 'AutoUpdateMentionsStarted' event if the process hasn't started`() {
            val expected = AutoUpdateMentionsStarted::class.withId(id)
            context().assertEvent(expected)
        }

        @Test
        internal fun `do nothing if the process has already started`() {
            emitTimePassedEvent()
            val event = GitHubTokenUpdated::class.withId(id)
            context().receivesEvent(event)
            context().assertEvents()
                .withType(AutoUpdateMentionsStarted::class.java)
                .hasSize(1)
        }
    }

    @Nested internal inner class
    `React on 'TimePassed' event, and` {

        @Nested internal inner class
        `If no update request has been made yet,` {

            @Test
            internal fun `send 'UpdateMentionsFromGitHub' command`() {
                val time = currentTime()
                emitTimePassedEvent(time)
                val expected = UpdateMentionsFromGitHub::class.buildBy(id, time)
                val commandSubject = context().assertCommands()
                    .withType(UpdateMentionsFromGitHub::class.java)
                commandSubject.hasSize(1)
                commandSubject.message(0)
                    .isEqualTo(expected)
            }

            @Test
            internal fun `set last mentions request time`() {
                val time = currentTime()
                emitTimePassedEvent(time)
                val expected = AutoUpdateMentions::class.with(id, time)
                context().assertState(id, expected)
            }
        }

        @Nested internal inner class
        `If required time between updates has passed since the last update,` {

            private lateinit var lastRequestTime: Timestamp

            @BeforeEach
            internal fun emitTwoTimePassedEvents() {
                val firstRequestTime = currentTime()
                lastRequestTime = firstRequestTime.add(mentionsUpdateInterval)
                emitTimePassedEvent(firstRequestTime)
                emitTimePassedEvent(lastRequestTime)
            }

            @Test
            internal fun `send 'UpdateMentionsFromGitHub' command`() {
                val expected = UpdateMentionsFromGitHub::class.buildBy(id, lastRequestTime)
                val commandSubject = context().assertCommands()
                    .withType(UpdateMentionsFromGitHub::class.java)
                commandSubject.hasSize(2)
                commandSubject.message(1)
                    .isEqualTo(expected)
            }

            @Test
            internal fun `update last mentions request time`() {
                val expected = AutoUpdateMentions::class.with(id, lastRequestTime)
                context().assertState(id, expected)
            }
        }
        
        @Test
        internal fun `do nothing if the required time between updates has not elapsed`() {
            val time = currentTime()
            emitTimePassedEvent(time)
            emitTimePassedEvent(time.add(seconds(1)))
            val expected = AutoUpdateMentions::class.with(id, time)
            context().assertCommands()
                .withType(UpdateMentionsFromGitHub::class.java)
                .hasSize(1)
            context().assertState(id, expected)
        }
    }

    private fun emitTimePassedEvent(time: Timestamp = currentTime()) {
        val clockContext = ThirdPartyContext.singleTenant("Clock")
        val event = TimePassed::class.buildBy(time)
        val actor = UserId::class.generate()
        clockContext.emittedEvent(event, actor)
    }
}
