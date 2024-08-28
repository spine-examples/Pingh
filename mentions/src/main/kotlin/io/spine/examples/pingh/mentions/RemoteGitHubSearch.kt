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
import com.google.protobuf.util.Timestamps
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpMessageBuilder
import io.ktor.http.HeadersBuilder
import io.ktor.http.HttpStatusCode
import io.spine.examples.pingh.github.Mention
import io.spine.examples.pingh.github.PersonalAccessToken
import io.spine.examples.pingh.github.Username
import io.spine.examples.pingh.github.fromFragment
import io.spine.examples.pingh.github.rest.CommentsResponse
import io.spine.examples.pingh.github.rest.IssuesAndPullRequestsSearchResult
import io.spine.examples.pingh.github.tag
import kotlin.jvm.Throws
import kotlinx.coroutines.runBlocking

/**
 * Using the GitHub API fetches mentions of a specific user.
 *
 * A user can be mentioned multiple times in comments or in the body of the item itself.
 * Each mention is treated as a separate entity and saved.
 *
 * @param engine The engine used to create the HTTP client.
 */
public class RemoteGitHubSearch(
    engine: HttpClientEngine
) : GitHubSearch {

    /**
     * HTTP client on behalf of which requests is made.
     */
    private val client = HttpClient(engine)

    /**
     * Searches for user `Mention`s made by others on GitHub in issues and pull requests,
     * as well as in comments under those items.
     *
     * The default value of `updateAfter` is `Timestamp.getDefaultInstance()`.
     *
     * @param username The name of the user whose mentions are to be fetched.
     * @param token The `PersonalAccessToken` to access user's private repositories.
     * @param updatedAfter The time after which GitHub items containing the searched mentions
     *   should have been updated.
     * @see [GitHubSearch.searchMentions]
     */
    @Throws(CannotObtainMentionsException::class)
    public override fun searchMentions(
        username: Username,
        token: PersonalAccessToken,
        updatedAfter: Timestamp
    ): Set<Mention> =
        findMentions(username, token, updatedAfter, ItemType.ISSUE) +
                findMentions(username, token, updatedAfter, ItemType.PULL_REQUEST)

    /**
     * Requests GitHub for mentions of a user in issues or pull requests,
     * then looks for where the user was specifically mentioned in that item.
     */
    @Throws(CannotObtainMentionsException::class)
    private fun findMentions(
        username: Username,
        token: PersonalAccessToken,
        updatedAfter: Timestamp,
        itemType: ItemType
    ): Set<Mention> {
        val userTag = username.tag()
        return searchIssuesOrPullRequests(username, token, updatedAfter, itemType)
            .itemList
            .flatMap { item ->
                val mentionsInComments = obtainCommentsByUrl(item.commentsUrl, token)
                    .itemList
                    .filter { comment -> comment.body.contains(userTag) }
                    .map { comment -> Mention::class.fromFragment(comment, item.title) }

                if (item.body.contains(userTag)) {
                    mentionsInComments.plus(Mention::class.fromFragment(item))
                } else {
                    mentionsInComments
                }
            }
            .toSet()
    }

    /**
     * Sends a request to GitHub API for searching issues or pull requests that
     * mention a particular user.
     *
     * There is only one endpoint for searching issues or pull requests from the GitHub API,
     * but when using GitHub Apps, it is mandatory to specify what is being searched
     * for using the `is:` filter. Therefore, the type of item being searched for is specified.
     *
     * @see <a href="https://docs.github.com/en/rest/search/search#search-issues-and-pull-requests">
     *     Search issues and pull requests</a>
     */
    @Throws(CannotObtainMentionsException::class)
    private fun searchIssuesOrPullRequests(
        username: Username,
        token: PersonalAccessToken,
        updatedAfter: Timestamp,
        itemType: ItemType
    ): IssuesAndPullRequestsSearchResult =
        runBlocking {
            val response = GitHubSearchRequest
                .get("https://api.github.com/search/issues")
                .by(itemType)
                .by(username)
                .by(updatedAfter)
                .with(token)
                .requestOnBehalfOf(client)

            if (response.status != HttpStatusCode.OK) {
                throw CannotObtainMentionsException(response.status.value)
            }
            IssuesAndPullRequestsSearchResult::class.parseJson(response.body())
        }

    /**
     * Sends a request to GitHub API to obtain comments on their URL previously received.
     */
    @Throws(CannotObtainMentionsException::class)
    private fun obtainCommentsByUrl(url: String, token: PersonalAccessToken): CommentsResponse =
        runBlocking {
            val response = client.get(url) {
                configureHeaders(token)
            }
            if (response.status != HttpStatusCode.OK) {
                throw CannotObtainMentionsException(response.status.value)
            }
            val json = response.body<String>()
            // The received JSON contains only an array, but Protobuf JSON Parser
            // cannot process it. So the array is converted to JSON, where the result
            // is just the value of the `item` field.
            CommentsResponse::class.parseJson("{ item: $json }")
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
        fun value(): String = gitHubName
    }

    /**
     * Builder for creating and sending request to search for mentions on GitHub.
     */
    private class GitHubSearchRequest private constructor(private val url: String) {

        companion object {
            /**
             * Creates builder and sets the request URL.
             */
            fun get(url: String): GitHubSearchRequest = GitHubSearchRequest(url)
        }

        /**
         * The type of the searched item.
         */
        private var itemType: ItemType? = null

        /**
         * The name of the user whose mentions are requested.
         */
        private var username: Username? = null

        /**
         * The time after which GitHub items containing the searched mentions
         * should have been updated.
         */
        private var updatedAfter: Timestamp? = null

        /**
         * The user authentication token on GitHub.
         */
        private var token: PersonalAccessToken? = null

        /**
         * Sets the type of the searched item.
         */
        fun by(itemType: ItemType): GitHubSearchRequest {
            this.itemType = itemType
            return this
        }

        /**
         * Sets the name of the user whose mentions are requested.
         */
        fun by(username: Username): GitHubSearchRequest {
            this.username = username
            return this
        }

        /**
         * Sets the time after which GitHub items containing the searched mentions
         * should have been updated.
         */
        fun by(updatedAfter: Timestamp): GitHubSearchRequest {
            this.updatedAfter = updatedAfter
            return this
        }

        /**
         * Sets the user authentication token on GitHub
         */
        fun with(token: PersonalAccessToken): GitHubSearchRequest {
            this.token = token
            return this
        }

        /**
         * Creates and sends request with specified data.
         *
         * @throws IllegalArgumentException some request data is not specified.
         */
        suspend fun requestOnBehalfOf(client: HttpClient): HttpResponse {
            checkNotNull(itemType) { "The type of the searched item is not specified." }
            checkNotNull(username) {
                "The name of the user whose mentions are requested is not specified."
            }
            checkNotNull(updatedAfter) {
                "The time after which GitHub items containing the searched mentions " +
                        "should have been updated is not specified."
            }
            checkNotNull(token) {
                "The user authentication token on GitHub is not specified."
            }

            val query = "is:${itemType!!.value()} mentions:${username!!.value} " +
                    "updated:>${Timestamps.toString(updatedAfter)}"
            return client.get(url) {
                url {
                    parameters.append("q", query)
                    parameters.append("per_page", "100")
                    parameters.append("sort", "updated")
                    parameters.append("order", "desc")
                }
                configureHeaders(token!!)
            }
        }
    }
}

/**
 * Configures headers for an HTTP request to the GitHub API.
 *
 * @see <a href="https://docs.github.com/en/rest/authentication/authenticating-to-the-rest-api">
 *     Authenticating to the GitHub REST API</a>
 */
private fun HttpMessageBuilder.configureHeaders(token: PersonalAccessToken): HeadersBuilder =
    headers.apply {
        append("Authorization", "Bearer ${token.value}")
        append("Accept", "application/vnd.github+json")
    }
