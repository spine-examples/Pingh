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

import com.google.common.annotations.VisibleForTesting
import io.spine.environment.Environment
import io.spine.environment.Tests
import io.spine.examples.pingh.sessions.GitHubUsers
import io.spine.server.BoundedContext
import io.spine.server.BoundedContextBuilder

/**
 * Configures Mentions bounded context.
 *
 * It is expected that the business scenarios
 * of the created context require access to the GitHub API.
 * Therefore, an instance of GitHub client is required
 * as a parameter.
 *
 * @property search Allows to access GitHub Search API.
 * @property users Allows to retrieve user information using the GitHub API.
 */
public class MentionsContext(
    private val search: GitHubSearch,
    private val users: GitHubUsers
) {
    /**
     * Whether to run a [janitor][MentionsJanitor] within the Mentions bounded context.
     *
     * When enabled, the janitor periodically removes entity records
     * marked as archived or deleted from the storage.
     *
     * The janitor is disabled in the test environment
     * and enabled by default in all other environments.
     */
    @VisibleForTesting
    internal var janitorEnabled = !Environment.instance().`is`(Tests::class.java)

    /**
     * Creates a new builder for the Mentions bounded context.
     *
     * The returned builder instance is already configured
     * to serve the entities which belong to this context.
     */
    public fun newBuilder(): BoundedContextBuilder {
        val mentionRepo = MentionRepository()
        val gitHubClientRepo = GitHubClientRepository(search, users)
        val contextBuilder = BoundedContext.singleTenant(name)
            .add(gitHubClientRepo)
            .add(mentionRepo)
            .add(UserMentionsRepository())
        if (janitorEnabled) {
            val janitorRepo = MentionsJanitorRepository(mentionRepo, gitHubClientRepo)
            contextBuilder.add(janitorRepo)
        }
        return contextBuilder
    }

    public companion object {
        /**
         * Name of the Mentions bounded context.
         */
        @VisibleForTesting
        public const val name: String = "Mentions"
    }
}
