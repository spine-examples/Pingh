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

package io.spine.examples.pingh.client.e2e.given

import io.spine.examples.pingh.github.Username
import io.spine.examples.pingh.mentions.MentionId
import io.spine.examples.pingh.mentions.MentionStatus
import io.spine.examples.pingh.mentions.MentionView
import io.spine.examples.pingh.mentions.of
import io.spine.examples.pingh.testing.mentions.given.loadTeamMentions
import io.spine.examples.pingh.testing.mentions.given.loadUserMentions

/**
 * Returns the predefined list of user mentions that occurred on GitHub.
 *
 * @return List of mentions in order by descending time of mention creation.
 */
internal fun expectedMentionsList(whoWasMentioned: Username): List<MentionView> =
    (loadUserMentions() + loadTeamMentions())
        .map { mention ->
            with(MentionView.newBuilder()) {
                id = MentionId::class.of(mention.id, whoWasMentioned)
                whoMentioned = mention.author
                title = mention.title
                whenMentioned = mention.whenMentioned
                url = mention.url
                status = MentionStatus.UNREAD
                whereMentioned = mention.whereMentioned
                if (mention.hasTeam()) {
                    viaTeam = mention.team
                }
                vBuild()
            }
        }
        .sortedByDescending { it.whenMentioned.seconds }

/**
 * Returns a random `MentionView` with `UNREAD` status from this list.
 *
 * @throws NoSuchElementException if this list has no unread mentions.
 * @see [Collection.random]
 */
internal fun List<MentionView>.randomUnread(): MentionView =
    this.filter { it.status == MentionStatus.UNREAD }
        .random()
