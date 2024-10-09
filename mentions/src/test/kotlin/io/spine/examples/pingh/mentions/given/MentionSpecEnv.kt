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

import com.google.protobuf.Timestamp
import io.spine.base.Time.currentTime
import io.spine.core.UserId
import io.spine.examples.pingh.github.NodeId
import io.spine.examples.pingh.github.User
import io.spine.examples.pingh.github.Username
import io.spine.examples.pingh.github.of
import io.spine.examples.pingh.mentions.Mention
import io.spine.examples.pingh.mentions.MentionId
import io.spine.examples.pingh.mentions.MentionStatus
import io.spine.examples.pingh.mentions.buildBy
import io.spine.examples.pingh.mentions.of
import io.spine.examples.pingh.mentions.event.MentionSnoozed
import io.spine.examples.pingh.mentions.event.MentionUnsnoozed
import io.spine.examples.pingh.mentions.event.UserMentioned
import io.spine.examples.pingh.mentions.rejection.Rejections.MentionIsAlreadyRead
import io.spine.net.Url
import io.spine.testing.TestValues.randomString
import java.util.Objects
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Creates a new `MentionId` with random ID of node and name of user that was mentioned.
 */
internal fun KClass<MentionId>.generate(): MentionId =
    of(
        NodeId::class.of(randomString()),
        Username::class.of(randomString())
    )

/**
 * Creates a new `UserMentioned` event with the specified ID of the mention.
 * Other event fields are set randomly.
 */
internal fun KClass<UserMentioned>.buildBy(id: MentionId): UserMentioned =
    UserMentioned.newBuilder()
        .setId(id)
        .setTitle(randomString())
        .setUrl(Url::class.of(randomString()))
        .setWhoMentioned(User::class.of(randomString(), randomString()))
        .setWhenMentioned(currentTime())
        .vBuild()

/**
 * Creates a new `Mention` with the specified ID, the status of this mention,
 * the time until which the mention is snoozed, and data contained in the `UserMentioned` event.
 */
internal fun KClass<Mention>.buildBy(
    id: MentionId,
    status: MentionStatus,
    event: UserMentioned,
    snoozedUntilWhen: Timestamp? = null
): Mention =
    with(Mention.newBuilder()) {
        this.id = id
        this.status = status
        whoMentioned = event.whoMentioned
        title = event.title
        whenMentioned = event.whenMentioned
        if (!Objects.equals(snoozedUntilWhen, null)) {
            this.snoozeUntilWhen = snoozedUntilWhen
        }
        vBuild()
    }

/**
 * Creates a new `MentionSnoozed` event with the specified ID of the mention.
 */
internal fun KClass<MentionSnoozed>.buildBy(id: MentionId): MentionSnoozed =
    this.buildBy(id, currentTime())

/**
 * Creates a new `MentionIsAlreadyRead` rejection with the specified ID of the mention.
 */
internal fun KClass<MentionIsAlreadyRead>.buildBy(id: MentionId): MentionIsAlreadyRead =
    MentionIsAlreadyRead.newBuilder()
        .setId(id)
        .vBuild()

/**
 * Creates a new `UserId` with the randomly specified value.
 */
internal fun KClass<UserId>.generate(): UserId =
    UserId.newBuilder()
        .setValue(UUID.randomUUID().toString())
        .vBuild()

/**
 * Creates a new `MentionUnsnoozed` event with passed ID of the mention.
 */
internal fun KClass<MentionUnsnoozed>.onlyWithId(id: MentionId): MentionUnsnoozed =
    MentionUnsnoozed.newBuilder()
        .setId(id)
        // Building the message partially to include only the tested fields.
        .buildPartial()
