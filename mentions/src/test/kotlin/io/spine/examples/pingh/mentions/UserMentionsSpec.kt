/*
 * Copyright 2025, TeamDev. All rights reserved.
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

import io.spine.examples.pingh.github.Username
import io.spine.examples.pingh.github.of
import io.spine.examples.pingh.mentions.event.MentionDeleted
import io.spine.examples.pingh.mentions.event.MentionPinned
import io.spine.examples.pingh.mentions.event.MentionRead
import io.spine.examples.pingh.mentions.event.MentionSnoozed
import io.spine.examples.pingh.mentions.event.MentionUnpinned
import io.spine.examples.pingh.mentions.event.MentionUnsnoozed
import io.spine.examples.pingh.mentions.event.UserMentioned
import io.spine.examples.pingh.mentions.given.buildBy
import io.spine.examples.pingh.mentions.given.from
import io.spine.examples.pingh.mentions.given.generateWith
import io.spine.examples.pingh.testing.mentions.given.PredefinedGitHubSearchResponses
import io.spine.examples.pingh.testing.sessions.given.PredefinedGitHubUsersResponses
import io.spine.server.BoundedContextBuilder
import io.spine.testing.TestValues.randomString
import io.spine.testing.server.blackbox.ContextAwareTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`UserMentions` should")
internal class UserMentionsSpec : ContextAwareTest() {

    private lateinit var id: UserMentionsId
    private lateinit var userMentioned: UserMentioned

    override fun contextBuilder(): BoundedContextBuilder = newMentionsContext(
        PredefinedGitHubSearchResponses(),
        PredefinedGitHubUsersResponses()
    )

    @BeforeEach
    internal fun emitUserMentionedEvent() {
        val username = Username::class.of(randomString())
        id = UserMentionsId::class.of(username)
        userMentioned = UserMentioned::class.generateWith(username)
        context().receivesEvent(userMentioned)
    }

    @Test
    internal fun `init 'UserMentions' state, and mark it as unread`() {
        assertMentionStatus(MentionStatus.UNREAD)
    }

    @Test
    internal fun `ignore 'UserMentioned' event if mention with this ID already exists`() {
        context().receivesEvent(userMentioned)
        assertMentionStatus(MentionStatus.UNREAD)
    }

    @Test
    internal fun `react on 'MentionSnoozed' event, and mark the target mention as snoozed`() {
        val event = MentionSnoozed::class.buildBy(userMentioned.id)
        context().receivesEvent(event)
        assertMentionStatus(MentionStatus.SNOOZED)
    }

    @Test
    internal fun `react on 'MentionUnsnoozed', and mark the target mention as unread`() {
        val snoozedEvent = MentionSnoozed::class.buildBy(userMentioned.id)
        val unsnoozedEvent = MentionUnsnoozed::class.from(userMentioned)
        context()
            .receivesEvent(snoozedEvent)
            .receivesEvent(unsnoozedEvent)
        assertMentionStatus(MentionStatus.UNREAD)
    }

    @Test
    internal fun `react on 'MentionRead' event, and mark the target mention as read`() {
        val event = MentionRead::class.buildBy(userMentioned.id)
        context().receivesEvent(event)
        assertMentionStatus(MentionStatus.READ)
    }

    private fun assertMentionStatus(status: MentionStatus) {
        val mention = MentionView::class.buildBy(userMentioned, status)
        val expected = UserMentions::class.buildBy(id, mention)
        context().assertState(id, expected)
    }

    @Test
    internal fun `react on 'MentionPinned' event, and mark the target mention as pinned`() {
        context().receivesEvent(
            MentionPinned::class.with(userMentioned.id),
        )
        assertMentionPinned(true)
    }

    @Test
    internal fun `react on 'MentionUnpinned' event, and mark the target mention as unpinned`() {
        context().receivesEvents(
            MentionPinned::class.with(userMentioned.id),
            MentionUnpinned::class.with(userMentioned.id)
        )
        assertMentionPinned(false)
    }

    private fun assertMentionPinned(pinned: Boolean) {
        val mention = MentionView::class.buildBy(userMentioned, MentionStatus.UNREAD, pinned)
        val expected = UserMentions::class.buildBy(id, mention)
        context().assertState(id, expected)
    }

    @Test
    internal fun `react on 'MentionDeleted' event, and remove obsolete mention`() {
        val newMention = UserMentioned::class.generateWith(id.username).run {
            context().receivesEvent(this)
            MentionView::class.buildBy(this, MentionStatus.UNREAD)
        }
        val event = MentionDeleted::class.with(userMentioned.id)
        context().receivesEvent(event)
        val expected = UserMentions::class.buildBy(id, newMention)
        context().assertState(id, expected)
    }
}
