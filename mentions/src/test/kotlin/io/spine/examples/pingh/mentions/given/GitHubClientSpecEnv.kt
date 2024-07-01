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

import com.google.protobuf.Timestamp
import io.spine.examples.pingh.github.PersonalAccessToken
import io.spine.examples.pingh.github.Username
import io.spine.examples.pingh.mentions.GitHubClient
import io.spine.examples.pingh.mentions.GitHubClientId
import io.spine.examples.pingh.mentions.MentionId
import io.spine.examples.pingh.mentions.buildBy
import io.spine.examples.pingh.mentions.command.UpdateMentionsFromGitHub
import io.spine.examples.pingh.mentions.event.UserMentioned
import io.spine.examples.pingh.mentions.rejection.GithubClientRejections.MentionsUpdateIsAlreadyInProgress
import io.spine.examples.pingh.sessions.SessionId
import io.spine.examples.pingh.sessions.buildBy
import io.spine.examples.pingh.sessions.event.UserLoggedIn
import io.spine.examples.pingh.testing.mentions.given.predefinedMentionsSet
import kotlin.reflect.KClass

/**
 * Creates a new `GitHubClient` with the specified `GitHubClientId` and `PersonalAccessToken`.
 */
internal fun KClass<GitHubClient>.buildBy(id: GitHubClientId, token: PersonalAccessToken):
        GitHubClient =
    GitHubClient.newBuilder()
        .setId(id)
        .setToken(token)
        .vBuild()

/**
 * Creates a new `GitHubClient` with the specified `GitHubClientId`, `PersonalAccessToken` and
 * the time when the last successful update occurred.
 */
internal fun KClass<GitHubClient>.buildBy(
    id: GitHubClientId,
    token: PersonalAccessToken,
    lastUpdate: Timestamp
): GitHubClient =
    GitHubClient.newBuilder()
        .setId(id)
        .setToken(token)
        .setWhenLastSuccessfullyUpdated(lastUpdate)
        .vBuild()

/**
 * Creates a new `GitHubClient` with the `when_started` field filled with the default value.
 */
internal fun KClass<GitHubClient>.buildWithDefaultWhenStartedField(): GitHubClient =
    GitHubClient.newBuilder()
        .setWhenStarted(Timestamp.getDefaultInstance())
        // Building the message partially to include
        // only the tested fields.
        .buildPartial()

/**
 * Creates a new `UserLoggedIn` event with the specified `Username` and `PersonalAccessToken`.
 */
internal fun KClass<UserLoggedIn>.buildBy(username: Username, token: PersonalAccessToken):
        UserLoggedIn =
    this.buildBy(
        SessionId::class.buildBy(username),
        token
    )

/**
 * Creates a new `UpdateMentionsFromGitHub` command with the specified ID of the `GitHubClient`
 * and time when the mentions update process is requested.
 */
internal fun KClass<UpdateMentionsFromGitHub>.buildBy(id: GitHubClientId, whenRequested: Timestamp):
        UpdateMentionsFromGitHub =
    UpdateMentionsFromGitHub.newBuilder()
        .setId(id)
        .setWhenRequested(whenRequested)
        .vBuild()

/**
 * Creates a new `MentionsUpdateIsAlreadyInProgress` rejection with the specified `GitHubClientId`.
 */
internal fun KClass<MentionsUpdateIsAlreadyInProgress>.buildBy(id: GitHubClientId):
        MentionsUpdateIsAlreadyInProgress =
    MentionsUpdateIsAlreadyInProgress.newBuilder()
        .setId(id)
        .vBuild()

/**
 * Reads mention data from a prepared JSON, converts it to `UserMentioned` events,
 * and returns their set.
 */
internal fun expectedUserMentionedSet(whoWasMentioned: Username): Set<UserMentioned> =
    predefinedMentionsSet()
        .map { mention ->
            with(UserMentioned.newBuilder()) {
                id = MentionId::class.buildBy(
                    mention.id,
                    whoWasMentioned
                )
                whoMentioned = mention.author
                title = mention.title
                whenMentioned = mention.whenMentioned
                url = mention.url
                vBuild()
            }
        }
        .toSet()
