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
import io.spine.examples.pingh.mentions.event.UserMentioned
import io.spine.server.projection.Projection

/**
 * The view of the mentions that have occurred.
 */
public class UserMentionsProjection :
    Projection<MentionId, UserMentions, UserMentions.Builder>() {

    /**
     * Creates the view when a user is mentioned.
     */
    @Subscribe
    internal fun on(event: UserMentioned) {
        with(builder()) {
            id = event.id
            whoWasMentioned = event.id.user
            whoMentioned = event.whoMentioned
            title = event.title
            whenMentioned = event.whenMentioned
            url = event.url
            status = MentionStatus.UNREAD
        }
    }

    /**
     * Marks this mention as snoozed.
     */
    @Subscribe
    internal fun on(event: MentionSnoozed) {
        builder().setStatus(MentionStatus.SNOOZED)
    }

    /**
     * Marks this mention as read.
     */
    @Subscribe
    internal fun on(event: MentionRead) {
        builder().setStatus(MentionStatus.READ)
    }
}
