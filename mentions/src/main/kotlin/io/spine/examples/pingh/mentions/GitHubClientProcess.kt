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

import io.spine.base.EventMessage
import io.spine.base.Time.currentTime
import io.spine.core.External
import io.spine.examples.pingh.github.Mention
import io.spine.examples.pingh.github.PersonalAccessToken
import io.spine.examples.pingh.mentions.command.UpdateMentionsFromGitHub
import io.spine.examples.pingh.mentions.event.GitHubTokenUpdated
import io.spine.examples.pingh.mentions.event.MentionsUpdateFromGitHubCompleted
import io.spine.examples.pingh.mentions.event.MentionsUpdateFromGitHubRequested
import io.spine.examples.pingh.mentions.event.UserMentioned
import io.spine.examples.pingh.mentions.rejection.CannotStartDataUpdateTooEarly
import io.spine.examples.pingh.mentions.rejection.MentionsUpdateIsAlreadyInProgress
import io.spine.examples.pingh.mentions.rejection.UsersGitHubTokenInvalid
import io.spine.examples.pingh.sessions.event.UserLoggedIn
import io.spine.server.command.Assign
import io.spine.server.event.React
import io.spine.server.procman.ProcessManager
import kotlin.jvm.Throws

/**
 * A process of reading user's mentions from GitHub.
 */
public class GitHubClientProcess :
    ProcessManager<GitHubClientId, GitHubClient, GitHubClient.Builder>() {

    /**
     * Service that fetches mentions from GitHub.
     *
     * Must necessarily be set in the [inject] method.
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
            .setId(GitHubClientId::class.buildBy(event.id.username))
            .setToken(event.token)
            .vBuild()
    }

    /**
     * Starts the process of updating mentions for the user.
     *
     * When a mention update is requested for a user, checks whether the previous update
     * has ended. If this condition is met, the event of the received request to update mentions
     * is emitted.
     */
    @Assign
    @Throws(
        CannotStartDataUpdateTooEarly::class,
        MentionsUpdateIsAlreadyInProgress::class,
        UsersGitHubTokenInvalid::class
    )
    internal fun handle(command: UpdateMentionsFromGitHub): MentionsUpdateFromGitHubRequested {
        if (state().hasWhenStarted()) {
            throw MentionsUpdateIsAlreadyInProgress.newBuilder()
                .setId(command.id)
                .build()
        }
        builder().setWhenStarted(currentTime())
        return MentionsUpdateFromGitHubRequested.newBuilder()
            .setId(state().id)
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
        val username = state().id.username
        val token = state().token
        val mentions = gitHubClientService.fetchMentions(username, token)
        val userMentionedEvents = createUserMentionedEvents(mentions)
        val mentionsUpdateFromGitHubCompleted = MentionsUpdateFromGitHubCompleted.newBuilder()
            .setId(state().id)
            .vBuild()
        builder().clearWhenStarted()
        return listOf(
            *userMentionedEvents.toTypedArray(),
            mentionsUpdateFromGitHubCompleted
        )
    }

    private fun createUserMentionedEvents(gitHubMentions: Set<Mention>): Set<EventMessage> =
        gitHubMentions
            .map { mention ->
                with(UserMentioned.newBuilder()) {
                    id = MentionId::class.buildBy(
                        mention.id,
                        state().id.username
                    )
                    whoMentioned = mention.whoMentioned
                    title = mention.title
                    whenMentioned = mention.whenMentioned
                    url = mention.url
                    vBuild()
                }
            }
            .toSet()

    /**
     * Supplies this instance of the process with a service allowing to access GitHub.
     *
     * It is expected this method is called right after the creation of the process instance.
     * Otherwise, the process will not be able to function properly.
     */
    internal fun inject(gitHubClientService: GitHubClientService) {
        this.gitHubClientService = gitHubClientService
    }
}
