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

package io.spine.examples.pingh.mentions

import com.google.protobuf.Timestamp
import io.spine.base.EventMessage
import io.spine.core.External
import io.spine.examples.pingh.github.Mention
import io.spine.examples.pingh.github.PersonalAccessToken
import io.spine.examples.pingh.github.Username
import io.spine.examples.pingh.mentions.command.UpdateMentionsFromGitHub
import io.spine.examples.pingh.mentions.event.GitHubTokenUpdated
import io.spine.examples.pingh.mentions.event.MentionsUpdateFromGitHubCompleted
import io.spine.examples.pingh.mentions.event.MentionsUpdateFromGitHubRequested
import io.spine.examples.pingh.mentions.event.RequestMentionsFromGitHubFailed
import io.spine.examples.pingh.mentions.event.UserMentioned
import io.spine.examples.pingh.mentions.rejection.MentionsUpdateIsAlreadyInProgress
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
     * It is expected this field is set by calling [inject]
     * right after the instance creation.
     */
    private lateinit var gitHubClientService: GitHubClientService

    /**
     * Updates the user's [PersonalAccessToken] each time the user logs in.
     */
    @React
    internal fun on(@External event: UserLoggedIn): GitHubTokenUpdated {
        builder().setToken(event.token)
        return GitHubTokenUpdated::class.buildBy(
            GitHubClientId::class.buildBy(event.id.username),
            event.token
        )
    }

    /**
     * Starts the process of updating mentions for the user.
     *
     * When a mention update is requested for a user, checks whether the previous update
     * has ended. If this condition is met, the process of updating the mentions from GitHub
     * is started.
     */
    @Assign
    @Throws(MentionsUpdateIsAlreadyInProgress::class)
    internal fun handle(command: UpdateMentionsFromGitHub): MentionsUpdateFromGitHubRequested {
        if (state().hasWhenStarted()) {
            throw MentionsUpdateIsAlreadyInProgress::class.buildBy(command.id)
        }
        builder().setWhenStarted(command.whenRequested)
        return MentionsUpdateFromGitHubRequested::class.buildBy(state().id)
    }

    /**
     * Fetches user's mentions from GitHub and terminates the mention update process.
     *
     * @return If the mentions fetching from GitHub is successful, list of events,
     * where the [UserMentioned] event for each mention comes first,
     * followed by a single [MentionsUpdateFromGitHubCompleted] event.
     * Otherwise, the list is one [RequestMentionsFromGitHubFailed] event.
     */
    @React
    internal fun on(event: MentionsUpdateFromGitHubRequested): List<EventMessage> {
        val username = state().id.username
        val token = state().token
        val updatedAfter = determineFromWhenFetchMentions(state().whenLastSuccessfulUpdate)
        val mentions = try {
            gitHubClientService.fetchMentions(username, token, updatedAfter)
        } catch (exception: CannotFetchMentionsFromGitHubException) {
            builder().clearWhenStarted()
            return listOf(
                RequestMentionsFromGitHubFailed::class.buildBy(state().id, exception.statusCode())
            )
        }
        val userMentionedEvents = toEvents(mentions, state().id.username)
        val mentionsUpdateFromGitHubCompleted =
            MentionsUpdateFromGitHubCompleted::class.buildBy(state().id)
        builder()
            .setWhenLastSuccessfulUpdate(state().whenStarted)
            .clearWhenStarted()
        return userMentionedEvents
            .toList<EventMessage>()
            .plus(mentionsUpdateFromGitHubCompleted)
    }

    /**
     * Supplies this instance of the process with a service allowing to access GitHub.
     *
     * It is expected this method is called right after the creation of the process instance.
     * Otherwise, the process will not be able to function properly.
     */
    internal fun inject(gitHubClientService: GitHubClientService) {
        this.gitHubClientService = gitHubClientService
    }

    private companion object {
        /**
         * Determines and returns the time from which mentions should be updated.
         *
         * If updates were already performed, it returns the time of the last successfully
         * completed update. If no updates are made, it returns the time equal to midnight
         * of the previous work day.
         */
        private fun determineFromWhenFetchMentions(whenLastSuccessfulUpdate: Timestamp): Timestamp {
            if (!whenLastSuccessfulUpdate.equals(whenLastSuccessfulUpdate.defaultInstanceForType)) {
                return whenLastSuccessfulUpdate
            }
            return identifyPreviousWorkday()
        }

        /**
         * Converts the set of `Mention`s to the set of `UserMentioned` events
         * with the specified name of the mentioned user.
         */
        private fun toEvents(gitHubMentions: Set<Mention>, whoWasMentioned: Username):
                Set<UserMentioned> =
            gitHubMentions
                .map { mention -> UserMentioned::class.buildBy(mention, whoWasMentioned) }
                .toSet()
    }
}
