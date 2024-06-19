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
import io.spine.examples.pingh.mentions.command.MarkMentionAsRead
import io.spine.examples.pingh.mentions.command.SnoozeMention
import io.spine.examples.pingh.mentions.event.MentionRead
import io.spine.examples.pingh.mentions.event.MentionSnoozed
import io.spine.examples.pingh.mentions.event.UserMentioned
import io.spine.examples.pingh.testing.github.given.PredefinedGitHubResponses
import io.spine.examples.pingh.mentions.given.buildBy
import io.spine.examples.pingh.mentions.given.generate
import io.spine.examples.pingh.mentions.rejection.Rejections.MentionIsAlreadyRead
import io.spine.server.BoundedContextBuilder
import io.spine.testing.server.blackbox.ContextAwareTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("`Mention` should")
public class MentionSpec : ContextAwareTest() {

    private lateinit var id: MentionId

    protected override fun contextBuilder(): BoundedContextBuilder =
        newMentionsContext(PredefinedGitHubResponses())

    @BeforeEach
    public fun emitUserMentionedEvent() {
        id = MentionId::class.generate()
        val event = UserMentioned::class.buildBy(id)
        context().receivesEvent(event)
    }

    @Nested
    public inner class `react on 'UserMentioned' event, and` {

        @Test
        public fun `init 'Mention' state and set 'UNREAD' status`() {
            val expected = Mention::class.buildBy(id, MentionStatus.UNREAD)
            context().assertState(id, expected)
        }
    }

    @Nested
    public inner class `handle 'SnoozeMention' command, and` {

        private lateinit var untilWhen: Timestamp

        @BeforeEach
        public fun sendSnoozeMentionCommand() {
            untilWhen = currentTime()
            val command = SnoozeMention::class.buildBy(id, untilWhen)
            context().receivesCommand(command)
        }

        @Test
        public fun `emit 'MentionSnoozed' event`() {
            val expected = MentionSnoozed::class.buildBy(id, untilWhen)
            context().assertEvent(expected)
        }

        @Test
        public fun `set 'SNOOZED' status in 'Mention' entity`() {
            val expected = Mention::class.buildBy(id, MentionStatus.SNOOZED)
            context().assertState(id, expected)
        }
    }

    @Nested
    public inner class `handle 'MarkMentionAsRead' command, and` {

        @BeforeEach
        public fun sendUserMentionAsReadCommand() {
            val command = MarkMentionAsRead::class.buildBy(id)
            context().receivesCommand(command)
        }

        @Test
        public fun `emit 'MentionRead' event`() {
            val expected = MentionRead::class.buildBy(id)
            context().assertEvent(expected)
        }

        @Test
        public fun `set 'READ' status in 'Mention' entity`() {
            val expected = Mention::class.buildBy(id, MentionStatus.READ)
            context().assertState(id, expected)
        }
    }

    @Test
    public fun `reject 'SnoozeMention' command, if mention is already read`() {
        val readCommand = MarkMentionAsRead::class.buildBy(id)
        val snoozeCommand = SnoozeMention::class.buildBy(id, currentTime())
        context()
            .receivesCommand(readCommand)
            .receivesCommand(snoozeCommand)
        val expected = MentionIsAlreadyRead::class.buildBy(id)
        context().assertEvent(expected)
    }

    @Test
    public fun `reject 'MarkMentionAsRead' command, if mention is already read`() {
        val firstReadCommand = MarkMentionAsRead::class.buildBy(id)
        val secondReadCommand = MarkMentionAsRead::class.buildBy(id)
        context()
            .receivesCommand(firstReadCommand)
            .receivesCommand(secondReadCommand)
        val expected = MentionIsAlreadyRead::class.buildBy(id)
        context().assertEvent(expected)
    }
}
