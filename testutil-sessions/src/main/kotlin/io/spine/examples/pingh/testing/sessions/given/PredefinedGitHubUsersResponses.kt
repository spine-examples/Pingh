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

package io.spine.examples.pingh.testing.sessions.given

import io.spine.examples.pingh.github.Organization
import io.spine.examples.pingh.github.PersonalAccessToken
import io.spine.examples.pingh.github.User
import io.spine.examples.pingh.github.Username
import io.spine.examples.pingh.github.from
import io.spine.examples.pingh.github.of
import io.spine.examples.pingh.sessions.GitHubUsers

/**
 * Implementation of `GitHubUsers` that loads responses from JSON files in the resource folder.
 *
 * Uses exclusively for testing.
 */
public class PredefinedGitHubUsersResponses : GitHubUsers {
    /**
     * The username that is returned when user information is retrieved.
     *
     * @see ownerOf
     */
    public var username: Username = Username::class.of("MykytaPimonovTD")

    /**
     * Whether the user is a member of any permitted organizations.
     */
    public var isMemberOfPermittedOrganizations: Boolean = true

    /**
     * Returns the `User` with the specified [username] and a default avatar URL.
     */
    override fun ownerOf(token: PersonalAccessToken): User =
        User::class.of(username.value, "https://avatars.githubusercontent.com/u/160486193")

    /**
     * Returns the set of `Organization`s retrieved from a JSON file in the resource folder
     * if the user is a member of any permitted organizations. Otherwise, returns empty set.
     */
    override fun memberships(token: PersonalAccessToken): Set<Organization> =
        if (isMemberOfPermittedOrganizations) {
            loadOrganizations()
                .itemList
                .map { fragment -> Organization::class.from(fragment) }
                .toSet()
        } else {
            setOf()
        }

    /**
     * Resets the instance to its initial state.
     *
     * Once the instance is reset, the user is considered a member of any permitted organizations,
     * and the username is set to the default value.
     */
    public fun reset() {
        isMemberOfPermittedOrganizations = true
        username = Username::class.of("MykytaPimonovTD")
    }
}
