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

import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper
import io.spine.examples.pingh.mentions.event.MentionDeleted
import io.spine.examples.pingh.mentions.event.MentionPinned
import io.spine.examples.pingh.mentions.event.MentionRead
import io.spine.examples.pingh.mentions.event.MentionSnoozed
import io.spine.examples.pingh.mentions.event.MentionUnpinned
import io.spine.examples.pingh.mentions.event.MentionUnsnoozed
import io.spine.examples.pingh.mentions.event.UserMentioned
import io.spine.server.projection.ProjectionRepository
import io.spine.server.route.EventRouting

/**
 * Manages instances of [UserMentionsProjection].
 */
internal class UserMentionsRepository :
    ProjectionRepository<UserMentionsId, UserMentionsProjection, UserMentions>() {

    @OverridingMethodsMustInvokeSuper
    override fun setupEventRouting(routing: EventRouting<UserMentionsId>) {
        super.setupEventRouting(routing)
        routing
            .route(UserMentioned::class.java) { event, _ -> toUserMentions(event.id) }
            .route(MentionSnoozed::class.java) { event, _ -> toUserMentions(event.id) }
            .route(MentionRead::class.java) { event, _ -> toUserMentions(event.id) }
            .route(MentionUnsnoozed::class.java) { event, _ -> toUserMentions(event.id) }
            .route(MentionPinned::class.java) { event, _ -> toUserMentions(event.id) }
            .route(MentionUnpinned::class.java) { event, _ -> toUserMentions(event.id) }
            .route(MentionDeleted::class.java) { event, _ -> toUserMentions(event.id) }
    }

    /**
     * Returns a set with a single `UserMentionsId`, that is created with
     * the name of the mentioned user from the passed `MentionId`.
     */
    private fun toUserMentions(mentionId: MentionId): Set<UserMentionsId> {
        return setOf(UserMentionsId::class.of(mentionId.user))
    }
}
