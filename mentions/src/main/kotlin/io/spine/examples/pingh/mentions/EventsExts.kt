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
import io.spine.examples.pingh.github.Mention
import io.spine.examples.pingh.github.PersonalAccessToken
import io.spine.examples.pingh.github.User
import io.spine.examples.pingh.github.Username
import io.spine.examples.pingh.mentions.event.AutoUpdateMentionsStarted
import io.spine.examples.pingh.mentions.event.GitHubTokenUpdated
import io.spine.examples.pingh.mentions.event.MentionRead
import io.spine.examples.pingh.mentions.event.MentionSnoozed
import io.spine.examples.pingh.mentions.event.MentionUnsnoozed
import io.spine.examples.pingh.mentions.event.MentionsUpdateFromGitHubCompleted
import io.spine.examples.pingh.mentions.event.MentionsUpdateFromGitHubRequested
import io.spine.examples.pingh.mentions.event.RequestMentionsFromGitHubFailed
import io.spine.examples.pingh.mentions.event.UserMentioned
import kotlin.reflect.KClass

/**
 * Creates a new `GitHubTokenUpdated` event with the specified `GitHubClientId`
 * and `PersonalAccessToken`.
 */
public fun KClass<GitHubTokenUpdated>.buildBy(id: GitHubClientId, token: PersonalAccessToken):
        GitHubTokenUpdated =
    GitHubTokenUpdated.newBuilder()
        .setId(id)
        .setToken(token)
        .vBuild()

/**
 * Creates a new `MentionsUpdateFromGitHubRequested` event with the specified `GitHubClientId`.
 */
public fun KClass<MentionsUpdateFromGitHubRequested>.buildBy(id: GitHubClientId):
        MentionsUpdateFromGitHubRequested =
    MentionsUpdateFromGitHubRequested.newBuilder()
        .setId(id)
        .vBuild()

/**
 * Creates a new `MentionsUpdateFromGitHubCompleted` event with the specified `GitHubClientId`.
 */
public fun KClass<MentionsUpdateFromGitHubCompleted>.buildBy(id: GitHubClientId):
        MentionsUpdateFromGitHubCompleted =
    MentionsUpdateFromGitHubCompleted.newBuilder()
        .setId(id)
        .vBuild()

/**
 * Creates a new `MentionRead` event with the specified ID of the mention.
 */
public fun KClass<MentionRead>.buildBy(id: MentionId): MentionRead =
    MentionRead.newBuilder()
        .setId(id)
        .vBuild()

/**
 * Creates a new `MentionSnoozed` event with the specified ID of the mention and
 * time to which the mention is snoozing.
 */
public fun KClass<MentionSnoozed>.buildBy(id: MentionId, untilWhen: Timestamp): MentionSnoozed =
    MentionSnoozed.newBuilder()
        .setId(id)
        .setUntilWhen(untilWhen)
        .vBuild()

/**
 * Creates a new `MentionUnsnoozed` event with the passed data.
 *
 * @param id The ID of the mention.
 * @param whoMentioned The user who created the mention.
 * @param title The mention's title.
 * @param whenMentioned The time when the user was mentioned.
 */
public fun KClass<MentionUnsnoozed>.with(
    id: MentionId,
    whoMentioned: User,
    title: String,
    whenMentioned: Timestamp
): MentionUnsnoozed =
    MentionUnsnoozed.newBuilder()
        .setId(id)
        .setWhoMentioned(whoMentioned)
        .setTitle(title)
        .setWhenMentioned(whenMentioned)
        .vBuild()

/**
 * Creates a new `RequestMentionsFromGitHubFailed` event with the specified `GitHubClientId`
 * and HTTP status code of the response from GitHub.
 */
public fun KClass<RequestMentionsFromGitHubFailed>.buildBy(id: GitHubClientId, statusCode: Int):
        RequestMentionsFromGitHubFailed =
    RequestMentionsFromGitHubFailed.newBuilder()
        .setId(id)
        .setResponseStatusCode(statusCode)
        .vBuild()

/**
 * Creates a new `UserMentioned` event with the name of the mentioned user
 * and the data specified in the `Mention`.
 */
public fun KClass<UserMentioned>.buildBy(mention: Mention, whoWasMentioned: Username):
        UserMentioned =
    UserMentioned.newBuilder()
        .setId(MentionId::class.buildBy(mention.id, whoWasMentioned))
        .setWhoMentioned(mention.author)
        .setTitle(mention.title)
        .setWhenMentioned(mention.whenMentioned)
        .setUrl(mention.url)
        .vBuild()

/**
 * Creates a new `AutoUpdateMentionsStarted` event with the passed `GitHubClientID`.
 */
internal fun KClass<AutoUpdateMentionsStarted>.withId(id: GitHubClientId):
        AutoUpdateMentionsStarted =
    AutoUpdateMentionsStarted.newBuilder()
        .setId(id)
        .vBuild()
