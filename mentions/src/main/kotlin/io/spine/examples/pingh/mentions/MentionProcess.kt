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

import io.spine.examples.pingh.mentions.command.MarkMentionAsRead
import io.spine.examples.pingh.mentions.command.SnoozeMention
import io.spine.examples.pingh.mentions.event.MentionRead
import io.spine.examples.pingh.mentions.event.MentionSnoozed
import io.spine.examples.pingh.mentions.event.UserMentioned
import io.spine.examples.pingh.mentions.rejection.MentionIsAlreadyRead
import io.spine.server.command.Assign
import io.spine.server.event.React
import io.spine.server.model.Nothing
import io.spine.server.procman.ProcessManager
import kotlin.jvm.Throws

/**
 * Coordinates the lifecycle of the mention, namely snoozing and reading.
 */
public class MentionProcess :
    ProcessManager<MentionId, Mention, Mention.Builder>() {

    /**
     * Initializes the mention when it occurred and indicates that mention is unread.
     */
    @React
    internal fun on(event: UserMentioned): Nothing {
        with(builder()) {
            id = event.id
            status = MentionStatus.UNREAD
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
        builder().setStatus(MentionStatus.SNOOZED)
        return with(MentionSnoozed.newBuilder()) {
            id = command.id
            untilWhen = command.untilWhen
            vBuild()
        }
    }

    /**
     * Completes the lifecycle of the mention, indicating that it has been read.
     */
    @Assign
    @Throws(MentionIsAlreadyRead::class)
    internal fun handle(command: MarkMentionAsRead): MentionRead {
        if (state().status == MentionStatus.READ) {
            throw MentionIsAlreadyRead::class.buildBy(command.id)
        }
        builder().setStatus(MentionStatus.READ)
        return with(MentionRead.newBuilder()) {
            id = command.id
            vBuild()
        }
    }
}
