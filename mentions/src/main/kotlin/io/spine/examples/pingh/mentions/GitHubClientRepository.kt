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

import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper
import io.spine.examples.pingh.sessions.SessionId
import io.spine.examples.pingh.sessions.UserSession
import io.spine.examples.pingh.sessions.event.UserLoggedIn
import io.spine.server.procman.ProcessManagerRepository
import io.spine.server.route.EventRouting

/**
 * Manages instances of [GitHubClientProcess].
 */
public class GitHubClientRepository :
    ProcessManagerRepository<GitHubClientId, GitHubClientProcess, GitHubClient>() {

    @OverridingMethodsMustInvokeSuper
    protected override fun setupEventRouting(routing: EventRouting<GitHubClientId>) {
        super.setupEventRouting(routing)
        routing
            .route(UserLoggedIn::class.java) { event, _ ->
                toGitHubClient(event.id)
            }
    }

    @OverridingMethodsMustInvokeSuper
    protected override fun configure(processManager: GitHubClientProcess) {
        super.configure(processManager)
    }

    /**
     * Returns id of [GitHubClient] of provided id of [UserSession] the same user.
     */
    private fun toGitHubClient(session: SessionId): Set<GitHubClientId> {
        return setOf(
            with(GitHubClientId.newBuilder()) {
                username = session.username
                vBuild()
            }
        )
    }
}
