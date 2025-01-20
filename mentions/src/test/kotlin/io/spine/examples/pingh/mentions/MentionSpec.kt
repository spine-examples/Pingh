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
import io.spine.base.Time.currentTime
import io.spine.core.UserId
import io.spine.examples.pingh.clock.buildBy
import io.spine.examples.pingh.clock.event.TimePassed
import io.spine.examples.pingh.mentions.MentionProcess.Companion.lifetimeOfUnreadMention
import io.spine.examples.pingh.mentions.command.MarkMentionAsRead
import io.spine.examples.pingh.mentions.command.PinMention
import io.spine.examples.pingh.mentions.command.SnoozeMention
import io.spine.examples.pingh.mentions.command.UnpinMention
import io.spine.examples.pingh.mentions.event.MentionDeleted
import io.spine.examples.pingh.mentions.event.MentionPinned
import io.spine.examples.pingh.mentions.event.MentionRead
import io.spine.examples.pingh.mentions.event.MentionSnoozed
import io.spine.examples.pingh.mentions.event.MentionUnpinned
import io.spine.examples.pingh.mentions.event.MentionUnsnoozed
import io.spine.examples.pingh.mentions.event.UserMentioned
import io.spine.examples.pingh.mentions.given.buildBy
import io.spine.examples.pingh.mentions.given.generate
import io.spine.examples.pingh.mentions.given.onlyWithId
import io.spine.examples.pingh.mentions.rejection.Rejections.MentionIsAlreadyRead
import io.spine.examples.pingh.testing.mentions.given.PredefinedGitHubSearchResponses
import io.spine.examples.pingh.testing.sessions.given.PredefinedGitHubUsersResponses
import io.spine.protobuf.Durations2.seconds
import io.spine.server.BoundedContextBuilder
import io.spine.server.integration.ThirdPartyContext
import io.spine.testing.server.blackbox.ContextAwareTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("`Mention` should")
internal class MentionSpec : ContextAwareTest() {

    private lateinit var id: MentionId
    private lateinit var userMentioned: UserMentioned

    override fun contextBuilder(): BoundedContextBuilder = newMentionsContext(
        PredefinedGitHubSearchResponses(),
        PredefinedGitHubUsersResponses()
    )

    @BeforeEach
    internal fun emitUserMentionedEvent() {
        id = MentionId::class.generate()
        userMentioned = UserMentioned::class.buildBy(id)
        context().receivesEvent(userMentioned)
    }

    @Nested internal inner class
    `React on 'UserMentioned' event, and` {

        @Test
        internal fun `init 'Mention' state and mark it as unread`() {
            val expected = Mention::class.buildBy(id, MentionStatus.UNREAD, userMentioned)
            context().assertState(id, expected)
        }
    }

    @Nested internal inner class
    `Handle 'SnoozeMention' command, and` {

        private lateinit var untilWhen: Timestamp

        @BeforeEach
        internal fun sendSnoozeMentionCommand() {
            untilWhen = currentTime()
            val command = SnoozeMention::class.buildBy(id, untilWhen)
            context().receivesCommand(command)
        }

        @Test
        internal fun `emit 'MentionSnoozed' event`() {
            val expected = MentionSnoozed::class.buildBy(id, untilWhen)
            context().assertEvent(expected)
        }

        @Test
        internal fun `snooze the target 'Mention', remembering the time until which it is snoozed`() {
            val expected = Mention::class.buildBy(
                id, MentionStatus.SNOOZED, userMentioned, untilWhen
            )
            context().assertState(id, expected)
        }
    }

    @Nested internal inner class
    `Handle 'MarkMentionAsRead' command, and` {

        @BeforeEach
        internal fun sendUserMentionAsReadCommand() {
            val command = MarkMentionAsRead::class.buildBy(id)
            context().receivesCommand(command)
        }

        @Test
        internal fun `emit 'MentionRead' event`() {
            val expected = MentionRead::class.buildBy(id)
            context().assertEvent(expected)
        }

        @Test
        internal fun `read the target 'Mention'`() {
            val expected = Mention::class.buildBy(id, MentionStatus.READ, userMentioned)
            context().assertState(id, expected)
        }
    }

    @Nested internal inner class
    `Handle 'PinMention' command, and` {

        @BeforeEach
        internal fun send() {
            val command = PinMention::class.with(id)
            context().receivesCommand(command)
        }

        @Test
        internal fun `emit 'MentionPinned' event`() {
            val expected = MentionPinned::class.with(id)
            context().assertEvent(expected)
        }

        @Test
        internal fun `mark mention as pinned`() {
            val expected = Mention::class.buildBy(
                id, MentionStatus.UNREAD, userMentioned, pinned = true
            )
            context().assertState(id, expected)
        }
    }

    @Nested internal inner class
    `Handle 'UnpinMention' command, and` {

        @BeforeEach
        internal fun send() {
            context().receivesCommands(
                PinMention::class.with(id),
                UnpinMention::class.with(id)
            )
        }

        @Test
        internal fun `emit 'MentionUnpinned' event`() {
            val expected = MentionUnpinned::class.with(id)
            context().assertEvent(expected)
        }

        @Test
        internal fun `mark mention as unpinned`() {
            val expected = Mention::class.buildBy(
                id, MentionStatus.UNREAD, userMentioned, pinned = false
            )
            context().assertState(id, expected)
        }
    }

    @Nested internal inner class
    `React on 'TimePassed' event, and` {

        @Nested internal inner class
        `If mention is unread,` {

            @Test
            internal fun `do nothing if mention is not obsolete`() {
                emitTimePassedEvent()
                context().assertEvents()
                    .withType(MentionDeleted::class.java)
                    .hasSize(0)
                val expected = Mention::class.buildBy(id, MentionStatus.UNREAD, userMentioned)
                context().assertState(id, expected)
            }

            @Test
            internal fun `do nothing if mention is obsolete and pinned`() {
                context().receivesCommand(PinMention::class.with(id))
                emitTimePassedEvent()
                context().assertEvents()
                    .withType(MentionDeleted::class.java)
                    .hasSize(0)
                val expected = Mention::class.buildBy(id, MentionStatus.UNREAD, userMentioned)
                context().assertState(id, expected)
            }

            @Test
            internal fun `emit 'MentionDeleted' event if mention is obsolete`() {
                emitTimePassedEvent(currentTime().add(lifetimeOfUnreadMention).add(seconds(1)))
                val expected = MentionDeleted::class.with(id)
                context().assertEvent(expected)
            }

            @Test
            internal fun `mark entity as deleted if mention is obsolete`() {
                emitTimePassedEvent(currentTime().add(lifetimeOfUnreadMention).add(seconds(1)))
                context().assertEntity(id, MentionProcess::class.java)
                    .deletedFlag()
                    .isTrue()
            }
        }

        @Nested internal inner class
        `If mention is read,` {

            @BeforeEach
            internal fun setReadStatus() {
                val command = MarkMentionAsRead::class.buildBy(id)
                context().receivesCommand(command)
            }

            @Test
            internal fun `do nothing if mention is not obsolete`() {
                emitTimePassedEvent()
                context().assertEvents()
                    .withType(MentionDeleted::class.java)
                    .hasSize(0)
                val expected = Mention::class.buildBy(id, MentionStatus.READ, userMentioned)
                context().assertState(id, expected)
            }

            @Test
            internal fun `do nothing if mention is obsolete and pinned`() {
                context().receivesCommand(PinMention::class.with(id))
                emitTimePassedEvent()
                context().assertEvents()
                    .withType(MentionDeleted::class.java)
                    .hasSize(0)
                val expected = Mention::class.buildBy(id, MentionStatus.READ, userMentioned)
                context().assertState(id, expected)
            }

            @Test
            internal fun `emit 'MentionDeleted' event if mention is obsolete`() {
                emitTimePassedEvent(
                    currentTime().add(MentionProcess.lifetimeOfReadMention).add(seconds(1))
                )
                val expected = MentionDeleted::class.with(id)
                context().assertEvent(expected)
            }

            @Test
            internal fun `mark entity as deleted if mention is obsolete`() {
                emitTimePassedEvent(
                    currentTime().add(MentionProcess.lifetimeOfReadMention).add(seconds(1))
                )
                context().assertEntity(id, MentionProcess::class.java)
                    .deletedFlag()
                    .isTrue()
            }
        }

        @Nested internal inner class
        `If mention is snoozed,` {

            @BeforeEach
            internal fun setSnoozedStatus() {
                val command = SnoozeMention::class.buildBy(id, currentTime())
                context().receivesCommand(command)
                emitTimePassedEvent()
            }

            @Test
            internal fun `emit 'MentionUnsnoozed' event`() {
                val expected = MentionUnsnoozed::class.onlyWithId(id)
                context().assertEvent(expected)
            }

            @Test
            internal fun `mark the target 'Mention' as unread`() {
                val expected = Mention::class.buildBy(id, MentionStatus.UNREAD, userMentioned)
                context().assertState(id, expected)
            }
        }

        @Nested internal inner class
        `If mention is deleted,` {

            @BeforeEach
            internal fun delete() {
                emitTimePassedEvent(currentTime().add(lifetimeOfUnreadMention).add(seconds(1)))
                val expected = MentionDeleted::class.with(id)
                context().assertEvent(expected)
                context().assertEntity(id, MentionProcess::class.java)
                    .deletedFlag()
                    .isTrue()
            }

            @Test
            internal fun `do not delete mention again`() {
                emitTimePassedEvent(currentTime().add(lifetimeOfUnreadMention).add(seconds(1)))
                context().assertEvents()
                    .withType(MentionDeleted::class.java)
                    // An event occurred when the mention was deleted.
                    .hasSize(1)
            }
        }

        @Test
        internal fun `do nothing if mention is read`() {
            val command = MarkMentionAsRead::class.buildBy(id)
            context().receivesCommand(command)
            emitTimePassedEvent()
            assertThatNothingHappened(MentionStatus.READ)
        }

        @Test
        internal fun `do nothing if mention is unread`() {
            emitTimePassedEvent()
            assertThatNothingHappened(MentionStatus.UNREAD)
        }

        @Test
        internal fun `do nothing if snooze time hasn't already passed`() {
            val command = SnoozeMention::class.buildBy(id, currentTime())
            context().receivesCommand(command)
            emitTimePassedEvent(Timestamps.MIN_VALUE)
            assertThatNothingHappened(MentionStatus.SNOOZED)
        }

        private fun emitTimePassedEvent(time: Timestamp = currentTime()) {
            val clockContext = ThirdPartyContext.singleTenant("Clock")
            val event = TimePassed::class.buildBy(time)
            val actor = UserId::class.generate()
            clockContext.emittedEvent(event, actor)
        }

        private fun assertThatNothingHappened(initialStatus: MentionStatus) {
            context().assertEvents()
                .withType(MentionUnsnoozed::class.java)
                .hasSize(0)
            val expected = Mention::class.buildBy(id, initialStatus, userMentioned)
            context().assertState(id, expected)
        }
    }

    @Test
    internal fun `reject 'SnoozeMention' command, if mention is already read`() {
        val readCommand = MarkMentionAsRead::class.buildBy(id)
        val snoozeCommand = SnoozeMention::class.buildBy(id, currentTime())
        context()
            .receivesCommand(readCommand)
            .receivesCommand(snoozeCommand)
        val expected = MentionIsAlreadyRead::class.buildBy(id)
        context().assertEvent(expected)
    }

    @Test
    internal fun `reject 'MarkMentionAsRead' command, if mention is already read`() {
        val firstReadCommand = MarkMentionAsRead::class.buildBy(id)
        val secondReadCommand = MarkMentionAsRead::class.buildBy(id)
        context()
            .receivesCommand(firstReadCommand)
            .receivesCommand(secondReadCommand)
        val expected = MentionIsAlreadyRead::class.buildBy(id)
        context().assertEvent(expected)
    }
}
