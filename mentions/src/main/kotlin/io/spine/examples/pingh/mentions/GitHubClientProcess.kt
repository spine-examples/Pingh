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

import io.spine.base.Time.currentTime
import io.spine.core.External
import io.spine.examples.pingh.github.PersonalAccessToken
import io.spine.examples.pingh.mentions.command.UpdateMentionsFromGitHub
import io.spine.examples.pingh.mentions.event.GitHubTokenUpdated
import io.spine.examples.pingh.mentions.event.MentionsUpdateFromGitHubCompleted
import io.spine.examples.pingh.mentions.event.MentionsUpdateFromGitHubRequested
import io.spine.examples.pingh.mentions.rejection.CannotStartDataUpdateTooEarly
import io.spine.examples.pingh.mentions.rejection.MentionsUpdateIsAlreadyInProgress
import io.spine.examples.pingh.mentions.rejection.UsersGitHubTokenInvalid
import io.spine.examples.pingh.sessions.event.UserLoggedIn
import io.spine.server.command.Assign
import io.spine.server.event.React
import io.spine.server.procman.ProcessManager
import kotlin.jvm.Throws

public class GitHubClientProcess :
    ProcessManager<GitHubClientId, GitHubClient, GitHubClient.Builder>() {

    @React
    internal fun on(@External event: UserLoggedIn): GitHubTokenUpdated {
        archived = true
        val id = with(GitHubClientId.newBuilder()) {
            username = event.id.username
            vBuild()
        }
        initState(id, event.token)

        return GitHubTokenUpdated.newBuilder()
            .setId(id)
            .setToken(event.token)
            .vBuild()
    }

    private fun initState(id: GitHubClientId, token: PersonalAccessToken) {
        builder()
            .setId(id)
            .setToken(token)
    }

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

        builder().setWhenStarted(currentTime())
        return MentionsUpdateFromGitHubRequested.newBuilder()
            .setId(command.id)
            .setToken(command.token)
            .vBuild()
    }

    @React
    internal fun on(event: MentionsUpdateFromGitHubRequested): MentionsUpdateFromGitHubCompleted {
        archived = true

        builder().clearWhenStarted()
        return MentionsUpdateFromGitHubCompleted.newBuilder()
            .setId(event.id)
            .vBuild()
    }
}
