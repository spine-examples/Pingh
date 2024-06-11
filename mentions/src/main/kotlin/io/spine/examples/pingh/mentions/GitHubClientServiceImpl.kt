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

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.http.Headers
import io.ktor.http.headers
import io.spine.examples.pingh.github.Mention
import io.spine.examples.pingh.github.PersonalAccessToken
import io.spine.examples.pingh.github.Username
import io.spine.examples.pingh.github.buildFromFragment
import io.spine.examples.pingh.github.rest.CommentsGetResult
import io.spine.examples.pingh.github.rest.IssuesAndPullRequestsSearchResult
import kotlin.jvm.Throws
import kotlinx.coroutines.runBlocking

/**
 * Using the GitHub API retrieves mentions of a specific user.
 *
 * A user can be mentioned multiple times in comments or in the body of the item itself.
 * Each mention is treated as a separate entity and saved.
 */
public class GitHubClientServiceImpl(
    engine: HttpClientEngine
) : GitHubClientService {

    /**
     * HTTP client on behalf of which requests is made.
     */
    private val client = HttpClient(engine)

    /**
     * Searches and returns user `Mention`s by `Username` that was mentioned
     * and their `PersonalAccessToken`.
     *
     * Mentions are searched in problems and pull requests.
     */
    @Throws(CannotFetchMentionsFromGitHubException::class)
    public override fun fetchMentions(username: Username, token: PersonalAccessToken):
            Set<Mention> =
        findMentions(username, token, ItemType.ISSUE) +
                findMentions(username, token, ItemType.PULL_REQUEST)

    /**
     * Requests GitHub for mentions of a user in issues or pull requests,
     * then looks for where the user was specifically mentioned in that item.
     */
    @Throws(CannotFetchMentionsFromGitHubException::class)
    private fun findMentions(
        username: Username,
        token: PersonalAccessToken,
        itemType: ItemType
    ): Set<Mention> {
        val mentions = mutableSetOf<Mention>()
        val userTag = "@${username.value}"
        val items = searchIssuesOrPullRequests(username, token, itemType)

        for (item in items.itemList) {
            if (item.body.contains(userTag)) {
                mentions.add(Mention::class.buildFromFragment(item))
            }
            val comments = getComments(item.commentsUrl, token)
            for (comment in comments.itemList) {
                if (comment.body.contains(userTag)) {
                    mentions.add(Mention::class.buildFromFragment(comment, item.title))
                }
            }
        }
        return mentions.toSet()
    }

    /**
     * Requests to GitHub for searching issues or pull requests that mention a particular user.
     *
     * There is only one endpoint for searching issues or pull requests from the GitHub API,
     * but when using GitHub Apps, it is mandatory to specify what is being searched
     * for using the `is:` filter. Therefore, the type of item being searched for is specified.
     *
     * @see <a href="https://docs.github.com/en/rest/search/search#search-issues-and-pull-requests">
     *     Search issues and pull requests</a>
     */
    @Throws(CannotFetchMentionsFromGitHubException::class)
    private fun searchIssuesOrPullRequests(
        username: Username,
        token: PersonalAccessToken,
        itemType: ItemType
    ): IssuesAndPullRequestsSearchResult =
        runBlocking {
            val response = client.get("https://api.github.com/search/issues") {
                url {
                    parameters.append(
                        "q",
                        "is:${itemType.gitHubName()} mentions:${username.value}"
                    )
                    parameters.append("per_page", "100")
                    parameters.append("sort", "updated")
                    parameters.append("order", "desc")
                }
                configureHeaders(token)
            }
            if (response.status != HttpStatusCode.OK) {
                throw CannotFetchMentionsFromGitHubException(response.status.value)
            }
            parseIssuesAndPullRequestsFromJson(response.body())
        }

    /**
     * Requests comments from GitHub on their URL previously received.
     */
    @Throws(CannotFetchMentionsFromGitHubException::class)
    private fun getComments(url: String, token: PersonalAccessToken): CommentsGetResult =
        runBlocking {
            val response = client.get(url) {
                configureHeaders(token)
            }
            if (response.status != HttpStatusCode.OK) {
                throw CannotFetchMentionsFromGitHubException(response.status.value)
            }
            val json = response.body<String>()
            // The received JSON contains only an array, but Protobuf JSON Parser
            // cannot process it. So the array is converted to JSON, where the result
            // is just the value of the `item` field.
            parseCommentsFromJson("{ item: $json }")
        }

    /**
     * Configures header for an HTTP request to the GitHub API.
     *
     * @see <a href="https://docs.github.com/en/rest/authentication/authenticating-to-the-rest-api">
     *     Authenticating to the GitHub REST API</a>
     */
    private fun configureHeaders(token: PersonalAccessToken): Headers =
        headers {
            append("Authorization", "Bearer ${token.value}")
            append("Accept", "application/vnd.github+json")
        }

    /**
     * GitHub item type. Contains the name by which they can be searching in the GitHub API
     * using the `is:` filter.
     */
    private enum class ItemType(private val gitHubName: String) {
        ISSUE("issue"),
        PULL_REQUEST("pull-request");

        /**
         * Returns GitHub name to search for an item of this type.
         */
        public fun gitHubName(): String = gitHubName
    }
}