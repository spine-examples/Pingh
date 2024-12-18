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

import com.google.common.annotations.VisibleForTesting
import com.google.protobuf.Timestamp
import com.google.protobuf.util.Durations
import io.spine.base.Time.currentTime
import io.spine.core.External
import io.spine.examples.pingh.clock.event.TimePassed
import io.spine.examples.pingh.mentions.command.MarkMentionAsRead
import io.spine.examples.pingh.mentions.command.PinMention
import io.spine.examples.pingh.mentions.command.SnoozeMention
import io.spine.examples.pingh.mentions.command.UnpinMention
import io.spine.examples.pingh.mentions.event.MentionArchived
import io.spine.examples.pingh.mentions.event.MentionPinned
import io.spine.examples.pingh.mentions.event.MentionRead
import io.spine.examples.pingh.mentions.event.MentionSnoozed
import io.spine.examples.pingh.mentions.event.MentionUnpinned
import io.spine.examples.pingh.mentions.event.MentionUnsnoozed
import io.spine.examples.pingh.mentions.event.UserMentioned
import io.spine.examples.pingh.mentions.rejection.MentionIsAlreadyRead
import io.spine.server.command.Assign
import io.spine.server.event.React
import io.spine.server.model.Nothing
import io.spine.server.procman.ProcessManager
import io.spine.server.tuple.EitherOf3
import kotlin.jvm.Throws

/**
 * Coordinates the lifecycle of the mention, namely snoozing and reading.
 */
internal class MentionProcess :
    ProcessManager<MentionId, Mention, Mention.Builder>() {

    /**
     * Creates the process for the mention occurred.
     *
     * The process is created in "unread" status.
     */
    @React
    internal fun on(event: UserMentioned): Nothing {
        with(builder()) {
            id = event.id
            whoMentioned = event.whoMentioned
            title = event.title
            whenMentioned = event.whenMentioned
            whereMentioned = event.whereMentioned
            status = MentionStatus.UNREAD
            viaTeam = event.viaTeam
        }
        return nothing()
    }

    /**
     * Puts the mention into snooze mode for the specified time.
     */
    @Assign
    @Throws(MentionIsAlreadyRead::class)
    internal fun handle(command: SnoozeMention): MentionSnoozed {
        if (state().status == MentionStatus.READ) {
            throw MentionIsAlreadyRead::class.buildBy(command.id)
        }
        with(builder()) {
            status = MentionStatus.SNOOZED
            snoozeUntilWhen = command.untilWhen
        }
        return MentionSnoozed::class.buildBy(
            command.id,
            command.untilWhen
        )
    }

    /**
     * Marks this mention as read by the mentioned user.
     */
    @Assign
    @Throws(MentionIsAlreadyRead::class)
    internal fun handle(command: MarkMentionAsRead): MentionRead {
        if (state().status == MentionStatus.READ) {
            throw MentionIsAlreadyRead::class.buildBy(command.id)
        }
        with(builder()) {
            status = MentionStatus.READ
            whenRead = currentTime()
            clearSnoozeUntilWhen()
        }
        return MentionRead::class.buildBy(command.id)
    }

    /**
     * Pins the mention at the top of the mentions list.
     */
    @Assign
    internal fun handle(command: PinMention): MentionPinned {
        builder().pinned = true
        return MentionPinned::class.with(command.id)
    }

    /**
     * Unpins the mention from the top of the mentions list.
     */
    @Assign
    internal fun handle(command: UnpinMention): MentionUnpinned {
        builder().pinned = false
        return MentionUnpinned::class.with(command.id)
    }

    /**
     * Updates the status of the mention based on the current time:
     *
     * 1. If the mention is snoozed and the snooze time has [expired][isSnoozeTimePassed],
     *   it [exits][unsnooze] the snooze state.
     *
     * 2. If the mention is [obsolete][isObsolete], it is [archived][archive].
     */
    @React
    internal fun on(
        @External event: TimePassed
    ): EitherOf3<MentionUnsnoozed, MentionArchived, Nothing> =
        when {
            isSnoozeTimePassed(event.time) -> EitherOf3.withA(unsnooze())
            isObsolete(event.time) -> EitherOf3.withB(archive())
            else -> EitherOf3.withC(nothing())
        }

    /**
     * Returns `true` if the mention is snoozed and its snooze time has expired.
     */
    private fun isSnoozeTimePassed(time: Timestamp): Boolean =
        state().status == MentionStatus.SNOOZED && time.isAfter(state().snoozeUntilWhen)

    /**
     * Marks this mention as unread when the snooze time passed.
     */
    private fun unsnooze(): MentionUnsnoozed {
        with(builder()) {
            status = MentionStatus.UNREAD
            clearSnoozeUntilWhen()
        }
        return MentionUnsnoozed::class.buildWith {
            id = state().id
            whoMentioned = state().whoMentioned
            title = state().title
            whenMentioned = state().whenMentioned
            whereMentioned = state().whereMentioned
            viaTeam = state().viaTeam
        }
    }

    /**
     * Returns `true` if the mention is obsolete.
     *
     * A mention is considered obsolete if:
     *
     * - It has been read,
     *   and the [lifetime of the read mention][lifetimeOfReadMention] has expired.
     *
     * - It has not been read,
     *   and the [lifetime of the unread mention][lifetimeOfUnreadMention] has expired.
     *
     * Pinned mentions never become obsolete.
     */
    private fun isObsolete(time: Timestamp): Boolean {
        if (state().pinned) {
            return false
        }
        return when (state().status) {
            MentionStatus.READ ->
                time.isAfter(state().whenRead.add(lifetimeOfReadMention))

            MentionStatus.UNREAD ->
                time.isAfter(state().whenMentioned.add(lifetimeOfUnreadMention))

            else -> false
        }
    }

    /**
     * Archives this mention.
     */
    private fun archive(): MentionArchived {
        archived = true
        return MentionArchived::class.with(state().id)
    }

    internal companion object {
        /**
         * The time during which a read mention is considered relevant.
         */
        @VisibleForTesting
        internal val lifetimeOfReadMention = Durations.fromDays(2)

        /**
         * The time during which an unread mention is considered relevant.
         */
        @VisibleForTesting
        internal val lifetimeOfUnreadMention = Durations.fromDays(7)
    }
}
