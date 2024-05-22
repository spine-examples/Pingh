/*
 * Copyright 2024, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import com.google.protobuf.Timestamp
import io.spine.base.EventMessage
import io.spine.base.Time.currentTime
import io.spine.core.External
import io.spine.examples.pingh.github.PersonalAccessToken
import io.spine.examples.pingh.github.User
import io.spine.examples.pingh.github.Username
import io.spine.examples.pingh.mentions.command.UpdateMentionsFromGitHub
import io.spine.examples.pingh.mentions.event.GitHubTokenUpdated
import io.spine.examples.pingh.mentions.event.MentionsUpdateFromGitHubCompleted
import io.spine.examples.pingh.mentions.event.MentionsUpdateFromGitHubRequested
import io.spine.examples.pingh.mentions.event.UserMentioned
import io.spine.examples.pingh.mentions.rejection.CannotStartDataUpdateTooEarly
import io.spine.examples.pingh.mentions.rejection.MentionsUpdateIsAlreadyInProgress
import io.spine.examples.pingh.mentions.rejection.UsersGitHubTokenInvalid
import io.spine.examples.pingh.sessions.event.UserLoggedIn
import io.spine.net.Url
import io.spine.server.command.Assign
import io.spine.server.event.React
import io.spine.server.procman.ProcessManager
import java.time.Instant
import kotlin.jvm.Throws

/**
 * Coordinates the updating of the specific user's mentions from GitHub.
 */
public class GitHubClientProcess :
    ProcessManager<GitHubClientId, GitHubClient, GitHubClient.Builder>() {

    /**
     * Service that fetches mentions from GitHub.
     */
    private lateinit var gitHubClientService: GitHubClientService

    /**
     * Updates the user's [PersonalAccessToken] each time the user logs in.
     */
    @React
    internal fun on(@External event: UserLoggedIn): GitHubTokenUpdated {
        archived = true
        builder().setToken(event.token)
        return GitHubTokenUpdated.newBuilder()
            .setId(builder().id)
            .setToken(event.token)
            .vBuild()
    }

    /**
     * Starts the process of updating mentions for the user.
     *
     * When a mention update is requested for a user, checks whether a minute has passed
     * since the previous successfully accepted [UpdateMentionsFromGitHub] command,
     * whether the previous update has ended, and whether the [PersonalAccessToken] is not expired.
     * If all the conditions are met, the event of the received request to update mentions
     * is emitted.
     */
    @Assign
    @Throws(
        CannotStartDataUpdateTooEarly::class,
        MentionsUpdateIsAlreadyInProgress::class,
        UsersGitHubTokenInvalid::class
    )
    internal fun handle(command: UpdateMentionsFromGitHub): MentionsUpdateFromGitHubRequested {

        if (builder().hasWhenStarted()) {
            throw MentionsUpdateIsAlreadyInProgress.newBuilder()
                .setId(command.id)
                .build()
        }

        // TODO:2024-05-21:mykyta.pimonov: Check that a minute has passed since
        //  the previous successfully accepted request.

        // TODO:2024-05-20:mykyta.pimonov: Check that the token is not expired.

        builder().setWhenStarted(currentTime())
        return MentionsUpdateFromGitHubRequested.newBuilder()
            .setId(command.id)
            .vBuild()
    }

    /**
     * Updates user mentions by emitting [UserMentioned] events
     * and terminates the mention update process.
     *
     * @return List of events, where the [UserMentioned] event for each mention comes first,
     * followed by a single [MentionsUpdateFromGitHubCompleted] event.
     */
    @React
    internal fun on(event: MentionsUpdateFromGitHubRequested): List<EventMessage> {
        archived = true
        val username = event.id.username
        val token = builder().token
        val mentions = gitHubClientService.fetchMentions(username, token)
        val emittingEvents = createUserMentionedEvents(mentions)
        emittingEvents.add(
            MentionsUpdateFromGitHubCompleted.newBuilder()
                .setId(event.id)
                .vBuild()
        )
        builder().clearWhenStarted()
        return emittingEvents.toList()
    }

    private fun createUserMentionedEvents(gitHubMentions: List<GitHubMention>):
            MutableList<EventMessage> =
        gitHubMentions
            .map { mention ->
                with(UserMentioned.newBuilder()) {
                    id = mentionIdBy(mention.id)
                    whoMentioned = userBy(
                        mention.whoMentioned.username,
                        mention.whoMentioned.avatarUrl
                    )
                    title = mention.title
                    whenMentioned = timestampBy(mention.whenCreated)
                    url = urlBy(mention.whoMentioned.avatarUrl)
                    vBuild()
                }
            }
            .toMutableList()

    private fun mentionIdBy(idValue: Long) =
        with(MentionId.newBuilder()) {
            uuid = "$idValue"
            vBuild()
        }

    private fun urlBy(specValue: String): Url =
        with(Url.newBuilder()) {
            spec = specValue
            vBuild()
        }

    private fun usernameBy(usernameValue: String): Username =
        with(Username.newBuilder()) {
            value = usernameValue
            vBuild()
        }

    private fun userBy(usernameValue: String, avatarUrlValue: String): User =
        with(User.newBuilder()) {
            username = usernameBy(usernameValue)
            avatarUrl = urlBy(avatarUrlValue)
            vBuild()
        }

    private fun timestampBy(timeValue: String): Timestamp {
        val instant = Instant.parse(timeValue)
        return with(Timestamp.newBuilder()) {
            seconds = instant.epochSecond
            nanos = instant.nano
            build()
        }
    }

    /**
     * Sets the implementation of [GitHubClientService].
     */
    internal fun inject(gitHubClientService: GitHubClientService) {
        this.gitHubClientService = gitHubClientService
    }
}
