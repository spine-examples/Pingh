/*
 * Copyright 2025, TeamDev. All rights reserved.
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

import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper
import io.spine.examples.pingh.clock.event.TimePassed
import io.spine.examples.pingh.janitor.PurgeableProcessManagerRepository
import io.spine.examples.pingh.sessions.GitHubUsers
import io.spine.examples.pingh.sessions.SessionId
import io.spine.examples.pingh.sessions.event.TokenUpdated
import io.spine.examples.pingh.sessions.event.UserLoggedIn
import io.spine.server.route.EventRouting

/**
 * Manages instances of [GitHubClientProcess].
 *
 * @property search The service that allows to access GitHub Search API.
 * @property users The service that allows to retrieve user information using the GitHub API.
 */
internal class GitHubClientRepository(
    private val search: GitHubSearch,
    private val users: GitHubUsers
) : PurgeableProcessManagerRepository<GitHubClientId, GitHubClientProcess, GitHubClient>() {

    @OverridingMethodsMustInvokeSuper
    override fun setupEventRouting(routing: EventRouting<GitHubClientId>) {
        super.setupEventRouting(routing)
        routing
            .route(UserLoggedIn::class.java) { event, _ ->
                toGitHubClientId(event.id)
            }
            .route(TokenUpdated::class.java) { event, _ ->
                toGitHubClientId(event.id)
            }
            .route(TimePassed::class.java) { _, _ -> toAll() }
    }

    @OverridingMethodsMustInvokeSuper
    override fun configure(processManager: GitHubClientProcess) {
        super.configure(processManager)
        processManager.inject(search, users)
    }

    /**
     * Returns a set with a single GitHub client ID, that corresponds to the passed user session.
     */
    private fun toGitHubClientId(session: SessionId): Set<GitHubClientId> {
        return setOf(GitHubClientId::class.of(session.username))
    }

    /**
     * Returns a set of identifiers of records in the process manager storage.
     */
    private fun toAll(): Set<GitHubClientId> =
        storage().index()
            .asSequence()
            .toSet()
}
