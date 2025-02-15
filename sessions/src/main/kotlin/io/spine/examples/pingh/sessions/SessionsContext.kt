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

package io.spine.examples.pingh.sessions

import io.spine.server.BoundedContext
import io.spine.server.BoundedContextBuilder

/**
 * Name of the Sessions [BoundedContext].
 */
public const val NAME: String = "Sessions"

/**
 * Configures Sessions [BoundedContext] with repositories.
 *
 * The returned builder instance is already configured to serve the entities which belong
 * to this context.
 *
 * It is expected that the business scenarios of the created context require access
 * to the GitHub REST API. Therefore, an instance of GitHub authentication server is required
 * as a parameter.
 *
 * @param auth The service that allows to access GitHub authentication API.
 * @param users The service that allows to retrieve user information using the GitHub API.
 */
public fun newSessionsContext(
    auth: GitHubAuthentication,
    users: GitHubUsers
): BoundedContextBuilder {
    val sessionRepo = UserSessionRepository(auth, users)
    val tokenMonitorRepo = TokenMonitorRepository()
    val janitorRepo = SessionsJanitorRepository(sessionRepo, tokenMonitorRepo)
    return BoundedContext.singleTenant(NAME)
        .add(sessionRepo)
        .add(tokenMonitorRepo)
        .add(janitorRepo)
}
