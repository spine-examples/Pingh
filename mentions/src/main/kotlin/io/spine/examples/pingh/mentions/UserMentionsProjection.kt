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

import io.spine.core.Subscribe
import io.spine.examples.pingh.mentions.event.MentionRead
import io.spine.examples.pingh.mentions.event.MentionSnoozed
import io.spine.examples.pingh.mentions.event.MentionUnsnoozed
import io.spine.examples.pingh.mentions.event.UserMentioned
import io.spine.server.projection.Projection

/**
 * The view of the mentions that have occurred.
 */
public class UserMentionsProjection :
    Projection<UserMentionsId, UserMentions, UserMentions.Builder>() {

    /**
     * Creates the view when a user is mentioned.
     *
     * The view is created with "unread" status.
     */
    @Subscribe
    internal fun on(event: UserMentioned) {
        builder()
            .addMention(
                MentionView::class.buildBy(event, MentionStatus.UNREAD)
            )
    }

    /**
     * Marks mention with the specified ID as snoozed.
     */
    @Subscribe
    internal fun on(event: MentionSnoozed) {
        setMentionStatus(event.id, MentionStatus.SNOOZED)
    }

    /**
     * Marks mention with the specified ID as read.
     */
    @Subscribe
    internal fun on(event: MentionRead) {
        setMentionStatus(event.id, MentionStatus.READ)
    }

    /**
     * Sets the status to unread when the snooze time of
     * the mention with the specified ID passed.
     */
    @Subscribe
    internal fun on(event: MentionUnsnoozed) {
        setMentionStatus(event.id, MentionStatus.UNREAD)
    }

    /**
     * Sets a new status for a mention by its ID.
     */
    private fun setMentionStatus(mentionId: MentionId, status: MentionStatus) {
        val idInList = builder()
            .mentionList
            .indexOfFirst { mention -> mention.id == mentionId }
        val updatedMention = builder()
            .mentionList[idInList]
            .toBuilder()
            .setStatus(status)
            .vBuild()
        builder().setMention(idInList, updatedMention)
    }
}
