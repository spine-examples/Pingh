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
import io.spine.examples.pingh.github.Mention
import io.spine.examples.pingh.github.NodeId
import io.spine.examples.pingh.github.Organization
import io.spine.examples.pingh.github.PersonalAccessToken
import io.spine.examples.pingh.github.Repo
import io.spine.examples.pingh.github.Team
import io.spine.examples.pingh.github.User
import io.spine.examples.pingh.github.Username
import io.spine.examples.pingh.github.loggedAs
import io.spine.examples.pingh.github.of
import io.spine.examples.pingh.mentions.GitHubClient
import io.spine.examples.pingh.mentions.GitHubClientId
import io.spine.examples.pingh.mentions.MentionId
import io.spine.examples.pingh.mentions.of
import io.spine.examples.pingh.mentions.command.UpdateMentionsFromGitHub
import io.spine.examples.pingh.mentions.event.UserMentioned
import io.spine.examples.pingh.mentions.rejection.GithubClientRejections.MentionsUpdateIsAlreadyInProgress
import io.spine.examples.pingh.testing.mentions.given.teamMentions
import io.spine.examples.pingh.testing.mentions.given.userMentions
import io.spine.net.Url
import io.spine.testing.TestValues.randomString
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
    (userMentions() + teamMentions())
        .map { mention ->
            with(UserMentioned.newBuilder()) {
                id = MentionId::class.of(
                    mention.id,
                    whoWasMentioned
                )
                whoMentioned = mention.author
                title = mention.title
                whenMentioned = mention.whenMentioned
                url = mention.url
                whereMentioned = mention.whereMentioned
                if (mention.hasTeam()) {
                    viaTeam = mention.team
                }
                vBuild()
            }
        }
        .toSet()

/**
 * Returns a new `Url` with randomly generated value.
 */
internal fun KClass<Url>.generate(): Url =
    Url::class.of("https://example.org/${randomString()}")

/**
 * Returns a new `Organization` with randomly generated login value.
 */
internal fun KClass<Organization>.generate(): Organization =
    Organization::class.loggedAs(randomString())

/**
 * Returns a new `Team` with randomly generated data.
 */
internal fun KClass<Team>.generate(): Team {
    val slug = randomString()
    return Team.newBuilder()
        .setName(slug)
        .setSlug(slug)
        .setOrg(Organization::class.generate())
        .vBuild()
}

/**
 * Returns a new `Mention` with randomly generated data.
 *
 * The mention can be modified using [modifier].
 */
internal fun KClass<Mention>.generate(modifier: Mention.Builder.() -> Unit = {}): Mention =
    with(Mention.newBuilder()) {
        id = NodeId::class.of(randomString())
        author = User::class.of(randomString(), randomString())
        title = randomString()
        whenMentioned = currentTime()
        url = Url::class.generate()
        whereMentioned = Repo::class.of(randomString(), randomString())
        modifier()
        vBuild()
    }
