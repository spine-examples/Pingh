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

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.spine.examples.pingh.github.Organization
import io.spine.examples.pingh.github.PersonalAccessToken
import io.spine.examples.pingh.github.User
import io.spine.examples.pingh.github.from
import io.spine.examples.pingh.github.rest.OrganizationsResponse
import kotlinx.coroutines.runBlocking

/**
 * Using the GitHub REST API obtains information of a specific user.
 *
 * @param engine The engine used to create the HTTP client.
 */
public class RemoteGitHubUsers(engine: HttpClientEngine) : GitHubUsers {
    /**
     * HTTP client on behalf of which requests is made.
     */
    private val client = HttpClient(engine)

    /**
     * Returns the owner of the provided access token.
     *
     * @param token The access token used to retrieve its owner.
     * @see <a href="https://shorturl.at/4LAR2">Get the authenticated user</a>
     */
    override fun ownerOf(token: PersonalAccessToken): User =
        runBlocking {
            val response = client.get("https://api.github.com/user") {
                configureHeaders(token)
            }
            User::class.parseJson(response.body())
        }

    /**
     * Returns the organizations of which the user owning
     * the provided access token is a member.
     *
     * Note that all organizations where the user is a public member are returned.
     * Organizations where the user is a private member are only obtained
     * if they have installed the
     * [Pingh GitHub App](https://github.com/apps/pingh-tracker-of-github-mentions).
     *
     * @param token The access token used to access organizations where
     *   the user is a private member.
     * @see <a href="https://shorturl.at/ileD9">List organizations for the authenticated user</a>
     */
    override fun memberships(token: PersonalAccessToken): Set<Organization> =
        runBlocking {
            val response = client.get("https://api.github.com/user/orgs") {
                configureHeaders(token)
            }
            val json = response.body<String>()
            // The received JSON contains only an array, but Protobuf JSON Parser
            // cannot process it. So the array is converted to JSON, where the result
            // is just the value of the `item` field.
            OrganizationsResponse::class.parseJson("{ item: $json }")
                .itemList
                .map {fragment -> Organization::class.from(fragment) }
                .toSet()
        }
}

/**
 * Configures headers for an HTTP request to the GitHub REST API.
 *
 * @see <a href="https://shorturl.at/sPHYj">Authenticating to the GitHub REST API</a>
 */
private fun HttpRequestBuilder.configureHeaders(token: PersonalAccessToken) {
    headers.append("Authorization", "Bearer ${token.value}")
    headers.append("Accept", "application/vnd.github+json")
}
