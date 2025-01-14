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

package io.spine.examples.pingh.mentions

import com.google.protobuf.Timestamp
import io.spine.base.Time.currentTime
import io.spine.examples.pingh.mentions.command.MarkMentionAsRead
import io.spine.examples.pingh.mentions.command.PinMention
import io.spine.examples.pingh.mentions.command.SnoozeMention
import io.spine.examples.pingh.mentions.command.UnpinMention
import io.spine.examples.pingh.mentions.command.UpdateMentionsFromGitHub
import kotlin.reflect.KClass

/**
 * Creates a new `MarkMentionAsRead` command with the specified ID of the mention.
 */
public fun KClass<MarkMentionAsRead>.buildBy(id: MentionId): MarkMentionAsRead =
    MarkMentionAsRead.newBuilder()
        .setId(id)
        .vBuild()

/**
 * Creates a new `SnoozeMention` command with the specified ID of the mention and
 * time to which the mention is snoozing.
 */
public fun KClass<SnoozeMention>.buildBy(id: MentionId, untilWhen: Timestamp): SnoozeMention =
    SnoozeMention.newBuilder()
        .setId(id)
        .setUntilWhen(untilWhen)
        .vBuild()

/**
 * Creates a new `UpdateMentionsFromGitHub` command with the specified `GitHubClientId`
 * and time when this update is requested.
 */
public fun KClass<UpdateMentionsFromGitHub>.buildBy(
    id: GitHubClientId,
    whenRequested: Timestamp = currentTime()
):
        UpdateMentionsFromGitHub =
    UpdateMentionsFromGitHub.newBuilder()
        .setId(id)
        .setWhenRequested(whenRequested)
        .vBuild()

/**
 * Creates a new `PinMention` command with the passed ID of the mention.
 */
public fun KClass<PinMention>.with(id: MentionId): PinMention =
    PinMention.newBuilder()
        .setId(id)
        .vBuild()

/**
 * Creates a new `UnpinMention` command with the passed ID of the mention.
 */
public fun KClass<UnpinMention>.with(id: MentionId): UnpinMention =
    UnpinMention.newBuilder()
        .setId(id)
        .vBuild()
