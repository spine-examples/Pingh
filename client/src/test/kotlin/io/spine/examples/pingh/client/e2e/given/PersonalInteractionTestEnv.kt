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
import io.spine.examples.pingh.mentions.buildBy
import io.spine.examples.pingh.testing.mentions.given.predefinedMentionsSet

internal fun expectedMentionsSet(whoWasMentioned: Username): Set<MentionView> =
    predefinedMentionsSet()
        .map { mention ->
            with(MentionView.newBuilder()) {
                id = MentionId::class.buildBy(mention.id, whoWasMentioned)
                whoMentioned = mention.author
                title = mention.title
                whenMentioned = mention.whenMentioned
                url = mention.url
                status = MentionStatus.UNREAD
                vBuild()
            }
        }
        .toSet()

internal fun Set<MentionView>.updateStatus(id: MentionId, status: MentionStatus):
        Set<MentionView> =
    this
        .map { mention ->
            if (mention.id != id) {
                return@map mention
            }
            mention.toBuilder()
                .setStatus(status)
                .vBuild()
        }
        .toSet()
