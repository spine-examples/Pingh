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

package io.spine.examples.pingh.mentions.given

import io.spine.base.Time.currentTime
import io.spine.core.UserId
import io.spine.examples.pingh.github.NodeId
import io.spine.examples.pingh.github.User
import io.spine.examples.pingh.github.Username
import io.spine.examples.pingh.github.buildBy
import io.spine.examples.pingh.mentions.Mention
import io.spine.examples.pingh.mentions.MentionId
import io.spine.examples.pingh.mentions.MentionStatus
import io.spine.examples.pingh.mentions.buildBy
import io.spine.examples.pingh.mentions.event.MentionSnoozed
import io.spine.examples.pingh.mentions.event.UserMentioned
import io.spine.examples.pingh.mentions.rejection.Rejections.MentionIsAlreadyRead
import io.spine.net.Url
import io.spine.testing.TestValues.randomString
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Creates a new `MentionId` with random ID of node and name of user that was mentioned.
 */
internal fun KClass<MentionId>.generate(): MentionId =
    this.buildBy(
        NodeId::class.buildBy(randomString()),
        Username::class.buildBy(randomString())
    )

/**
 * Creates a new `UserMentioned` event with the specified ID of the mention.
 * Other event fields are set randomly.
 */
internal fun KClass<UserMentioned>.buildBy(id: MentionId): UserMentioned =
    UserMentioned.newBuilder()
        .setId(id)
        .setTitle(randomString())
        .setUrl(Url::class.buildBy(randomString()))
        .setWhoMentioned(User::class.buildBy(randomString(), randomString()))
        .setWhenMentioned(currentTime())
        .vBuild()

/**
 * Creates a new `Mention` with the specified ID and the status of this mention.
 */
internal fun KClass<Mention>.buildBy(id: MentionId, status: MentionStatus): Mention =
    Mention.newBuilder()
        .setId(id)
        .setStatus(status)
        .vBuild()

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
