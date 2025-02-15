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

package io.spine.examples.pingh.sessions

import io.spine.examples.pingh.github.Organization
import io.spine.examples.pingh.github.PersonalAccessToken
import io.spine.examples.pingh.github.Team
import io.spine.examples.pingh.github.User

/**
 * Allows to retrieve user information using the GitHub REST API.
 */
public interface GitHubUsers {
    /**
     * Returns the owner of the passed token.
     *
     * @param token The token issued to the user whose information are being retrieved.
     */
    public fun ownerOf(token: PersonalAccessToken): User

    /**
     * Returns organizations the owner of the passed token belongs to.
     *
     * @param token The access token for retrieving organizations
     *   where the user is a private member.
     */
    public fun memberships(token: PersonalAccessToken): Set<Organization>

    /**
     * Returns the teams of which the token owner is a member.
     *
     * @param token The access token for retrieving teams
     *   where the user is a member.
     */
    public fun teamMemberships(token: PersonalAccessToken): Set<Team>
}
