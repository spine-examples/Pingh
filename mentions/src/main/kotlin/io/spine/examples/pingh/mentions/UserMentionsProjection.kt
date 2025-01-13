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
import io.spine.examples.pingh.mentions.event.MentionArchived
import io.spine.examples.pingh.mentions.event.MentionPinned
import io.spine.examples.pingh.mentions.event.MentionRead
import io.spine.examples.pingh.mentions.event.MentionSnoozed
import io.spine.examples.pingh.mentions.event.MentionUnpinned
import io.spine.examples.pingh.mentions.event.MentionUnsnoozed
import io.spine.examples.pingh.mentions.event.UserMentioned
import io.spine.logging.Logging
import io.spine.server.projection.Projection

/**
 * The view of the mentions that have occurred.
 */
internal class UserMentionsProjection :
    Projection<UserMentionsId, UserMentions, UserMentions.Builder>(), Logging {

    /**
     * Creates the view when a user is mentioned.
     *
     * The view is created with "unread" status.
     */
    @Subscribe
    internal fun on(event: UserMentioned) {
        val id = UserMentionsId::class.of(event.id.user)
        if (state().mentionList.any { it.id.equals(event.id) }) {
            _warn().log(
                "${id.forLog()}: A user mention occurred, but it was already saved. " +
                        "Since duplicates are not allowed, this mention is ignored. " +
                        "Mention ID: ${event.id.forLog()}."
            )
            return
        }
        builder()
            .addMention(
                MentionView::class.buildBy(event, MentionStatus.UNREAD)
            )
        _debug().log("${id.forLog()}: Mention with ID ${event.id.forLog()} saved.")
    }

    /**
     * Marks mention with the specified ID as snoozed.
     */
    @Subscribe
    internal fun on(event: MentionSnoozed) {
        modify(event.id) {
            status = MentionStatus.SNOOZED
        }
        _debug().log("${state().id.forLog()}: Mention with ID ${event.id.forLog()} snoozed.")
    }

    /**
     * Marks mention with the specified ID as read.
     */
    @Subscribe
    internal fun on(event: MentionRead) {
        modify(event.id) {
            status = MentionStatus.READ
        }
        _debug().log("${state().id.forLog()}: Mention with ID ${event.id.forLog()} read.")
    }

    /**
     * Sets the status to unread when the snooze time of
     * the mention with the specified ID passed.
     */
    @Subscribe
    internal fun on(event: MentionUnsnoozed) {
        modify(event.id) {
            status = MentionStatus.UNREAD
        }
        _debug().log("${state().id.forLog()}: Mention with ID ${event.id.forLog()} unsnoozed.")
    }

    /**
     * Marks mention with the specified ID as pinned.
     */
    @Subscribe
    internal fun on(event: MentionPinned) {
        modify(event.id) {
            pinned = true
        }
        _debug().log("${state().id.forLog()}: Mention with ID ${event.id.forLog()} pinned.")
    }

    /**
     * Marks the mention with the specified ID as unpinned.
     */
    @Subscribe
    internal fun on(event: MentionUnpinned) {
        modify(event.id) {
            pinned = false
        }
        _debug().log("${state().id.forLog()}: Mention with ID ${event.id.forLog()} unpinned.")
    }

    /**
     * Applies the [modifier] to the builder of the mention with specified [ID][id].
     */
    private fun modify(id: MentionId, modifier: MentionView.Builder.() -> Unit) {
        val idInList = builder()
            .mentionList
            .indexOfFirst { mention -> mention.id == id }
        val updatedMention = builder()
            .mentionList[idInList]
            .toBuilder()
            .run {
                modifier()
                vBuild()
            }
        builder().setMention(idInList, updatedMention)
    }

    /**
     * Removes a mention from the user's mentions list once it has been archived.
     */
    @Subscribe
    internal fun on(event: MentionArchived) {
        with(builder()) {
            val id = mentionList.indexOfFirst { it.id.equals(event.id) }
            if (id == -1) {
                _warn().log(
                    "${state().id}: The mention was not in the user's list, " +
                            "but an attempt was made to remove it. " +
                            "Mention ID: ${event.id.forLog()}."
                )
                return
            }
            removeMention(id)
            _debug().log(
                "${state().id.forLog()}: Mention with ID ${event.id.forLog()} was deleted " +
                        "from the projection because it is obsolete."
            )
        }
    }
}
