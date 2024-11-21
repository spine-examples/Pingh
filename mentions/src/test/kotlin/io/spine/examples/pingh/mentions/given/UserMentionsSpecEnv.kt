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

@file:Suppress("UnusedReceiverParameter" /* Class extensions don't use class as a parameter. */)

package io.spine.examples.pingh.mentions.given

import io.spine.base.Time.currentTime
import io.spine.examples.pingh.github.NodeId
import io.spine.examples.pingh.github.Repo
import io.spine.examples.pingh.github.User
import io.spine.examples.pingh.github.Username
import io.spine.examples.pingh.github.of
import io.spine.examples.pingh.mentions.MentionId
import io.spine.examples.pingh.mentions.MentionView
import io.spine.examples.pingh.mentions.UserMentions
import io.spine.examples.pingh.mentions.UserMentionsId
import io.spine.examples.pingh.mentions.of
import io.spine.examples.pingh.mentions.event.MentionUnsnoozed
import io.spine.examples.pingh.mentions.event.UserMentioned
import io.spine.net.Url
import io.spine.testing.TestValues.randomString
import kotlin.reflect.KClass

/**
 * Creates a new `UserMentioned` event with the specified name of the user who was mentioned.
 * Other event fields are set randomly.
 */
internal fun KClass<UserMentioned>.generateWith(whoWasMentioned: Username): UserMentioned =
    with(UserMentioned.newBuilder()) {
        id = MentionId::class.of(
            NodeId::class.of(randomString()),
            whoWasMentioned
        )
        whoMentioned = User::class.of(randomString(), randomString())
        title = randomString()
        whenMentioned = currentTime()
        url = Url::class.of(randomString())
        whereMentioned = Repo::class.of(randomString(), randomString())
        vBuild()
    }

/**
 * Creates a new `UserMentions` with the specified ID and mention's views.
 */
internal fun KClass<UserMentions>.buildBy(
    userMentionsId: UserMentionsId,
    vararg mentions: MentionView
): UserMentions =
    with(UserMentions.newBuilder()) {
        id = userMentionsId
        for (mention in mentions) {
            addMention(mention)
        }
        vBuild()
    }

/**
 * Creates a new `MentionUnsnoozed` event with the data from the passed event.
 */
internal fun KClass<MentionUnsnoozed>.from(event: UserMentioned): MentionUnsnoozed =
    with(MentionUnsnoozed.newBuilder()) {
        id = event.id
        whoMentioned = event.whoMentioned
        title = event.title
        whenMentioned = event.whenMentioned
        vBuild()
    }
