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
import io.spine.examples.pingh.github.Repo
import io.spine.examples.pingh.github.Team
import io.spine.examples.pingh.github.Username
import io.spine.examples.pingh.github.from
import io.spine.examples.pingh.github.fullName
import io.spine.examples.pingh.github.repo
import io.spine.examples.pingh.github.rest.CommentsResponse
import io.spine.examples.pingh.github.rest.IssueOrPullRequestFragment
import io.spine.examples.pingh.github.rest.IssuesAndPullRequestsSearchResponse
import io.spine.examples.pingh.github.rest.ReviewsResponse
import io.spine.examples.pingh.github.tag
import io.spine.logging.Logging
import kotlin.jvm.Throws
import kotlinx.coroutines.runBlocking

/**
 * The number of search results on a single page.
 *
 * @see <a href="https://shorturl.at/w35Ao">Changing the number of items per page</a>
 */
private const val perPage = 20

/**
 * Using the GitHub API fetches mentions of a specific user.
 *
 * A user can be mentioned multiple times in comments or in the body of the item itself.
 * Each mention is treated as a separate entity and saved.
 *
 * @param engine The engine used to create the HTTP client.
 */
public class RemoteGitHubSearch(engine: HttpClientEngine) : GitHubSearch, Logging {
    /**
     * HTTP client on behalf of which requests is made.
     */
    private val client = HttpClient(engine)

    /**
     * Searches for user `Mention`s made by others on GitHub in issues and pull requests,
     * as well as in comments under those items.
     *
     * The default value of `updateAfter` is `Timestamp.getDefaultInstance()`.
     * The default value of `limit` is `null`.
     *
     * @param username The name of the user whose mentions are to be fetched.
     * @param token The `PersonalAccessToken` to access user's private repositories.
     * @param updatedAfter The time after which GitHub items containing the searched mentions
     *   should have been updated.
     * @param limit The maximum number of recent mentions to return. If set, no more than `limit`
     *   mentions will be retrieved. If not set, all mentions since
     *   the [updatedAfter] will be returned.
     * @see [GitHubSearch.searchMentions]
     */
    @Throws(CannotObtainMentionsException::class)
    public override fun searchMentions(
        username: Username,
        token: PersonalAccessToken,
        updatedAfter: Timestamp,
        limit: Int?
    ): Set<Mention> = searchMentions(
        MentionTarget(username),
        token,
        updatedAfter,
        limit
    ) { setUser(username) }

    /**
     * Searches for team mentions on GitHub in issues and pull requests,
     * as well as in comments under those items.
     *
     * Note, mentions of a team are only found
     * if the [token] owner is a member of the team.
     *
     * The default value of `updateAfter` is `Timestamp.getDefaultInstance()`.
     * The default value of `limit` is `null`.
     *
     * @param team The team whose mention is being searched for.
     * @param token The `PersonalAccessToken` to access team mentions.
     * @param updatedAfter The time after which GitHub items containing the searched mentions
     *   should have been updated.
     * @param limit The maximum number of recent mentions to return. If set, no more than `limit`
     *   mentions will be retrieved. If not set, all mentions since
     *   the [updatedAfter] will be returned.
     */
    @Throws(CannotObtainMentionsException::class)
    override fun searchMentions(
        team: Team,
        token: PersonalAccessToken,
        updatedAfter: Timestamp,
        limit: Int?
    ): Set<Mention> = searchMentions(
        MentionTarget(team),
        token,
        updatedAfter,
        limit
    ) { setTeam(team) }

    /**
     * Searches for mentions of [target] on GitHub in issues and pull requests,
     * as well as in comments under those items.
     */
    @Throws(CannotObtainMentionsException::class)
    private fun searchMentions(
        target: MentionTarget,
        token: PersonalAccessToken,
        updatedAfter: Timestamp,
        limit: Int?,
        mentionModifier: Mention.Builder.() -> Unit = {}
    ): Set<Mention> {
        require(limit == null || limit > 0) {
            "The maximum number of recent mentions must be `null` or positive value, " +
                    "but $limit was passed."
        }
        return ItemType.entries
            .flatMap { type -> findMentions(target, token, updatedAfter, limit, type) }
            .run {
                if (limit != null) {
                    sortedByDescending { Timestamps.toNanos(it.whenMentioned) }.take(limit)
                } else {
                    this
                }
            }
            .map {
                with(it.toBuilder()) {
                    mentionModifier()
                    vBuild()
                }
            }
            .toSet()
    }

    /**
     * Requests GitHub for mentions of a [target] in issues or pull requests,
     * then looks for where the `target` was specifically mentioned on that item.
     *
     * The maximum number of fetched mentions can be restricted by the specified [limit];
     * if no `limit` is set, all mentions since the [updatedAfter] are retrieved.
     *
     * Accounts for pagination during the search. If not all results within `limit` are retrieved
     * in a single request, additional requests are made to ensure all results are fetched.
     */
    @Throws(CannotObtainMentionsException::class)
    private fun findMentions(
        target: MentionTarget,
        token: PersonalAccessToken,
        updatedAfter: Timestamp,
        limit: Int?,
        itemType: ItemType
    ): Set<Mention> {
        var page = 1
        val firstResult = searchIssuesOrPullRequests(target, token, updatedAfter, itemType, page)
        val totalCount = firstResult.totalCount
        val items = firstResult.itemList.toMutableSet()
        while ((limit == null || perPage * page < limit) && perPage * page < totalCount) {
            page++
            items += searchIssuesOrPullRequests(target, token, updatedAfter, itemType, page)
                .itemList
        }
        return items.filterMentions(target, token, updatedAfter, itemType)
    }

    /**
     * Sends a request to GitHub API for searching issues or pull requests that
     * mention a particular [target].
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
        target: MentionTarget,
        token: PersonalAccessToken,
        updatedAfter: Timestamp,
        itemType: ItemType,
        page: Int
    ): IssuesAndPullRequestsSearchResponse =
        runBlocking {
            val response = client
                .search("https://api.github.com/search/issues")
                .of(target)
                .by(itemType)
                .by(updatedAfter)
                .with(token)
                .onPage(page)
                .get()

            val json = response.body<String>()
            if (response.status != HttpStatusCode.OK) {
                _error().log(
                    "An error occurred while requesting mentions of ${target.tag}, " +
                            "and GitHub responded with: " +
                            "${json.replace("(\\r\\n|\\r|\\n)".toRegex(), "")}."
                )
                throw CannotObtainMentionsException(response.status.value)
            }
            IssuesAndPullRequestsSearchResponse::class.parseJson(json)
        }

    /**
     * Selects items where the [target] is mentioned and searches for mentions
     * in comments and reviews, if available.
     */
    private fun Set<IssueOrPullRequestFragment>.filterMentions(
        target: MentionTarget,
        token: PersonalAccessToken,
        updatedAfter: Timestamp,
        itemType: ItemType
    ): Set<Mention> =
        this.flatMap { item ->
            val mentions = client
                .findMentionsOn(item.repo(), item.number, itemType)
                .of(target)
                .with(token)
                .by(updatedAfter)
                .setTitle(item.title)
                .get()
            item.takeIf { it.body.contains(target.tag) }
                ?.let { Mention::class.from(it) }
                ?.takeIf { it.whenMentioned > updatedAfter }
                ?.let { mentions + it }
                ?: mentions
        }.toSet()
}

/**
 * GitHub item type.
 *
 * Contains the name by which they can be searching in the GitHub REST API using the `is:` filter.
 */
private enum class ItemType(val value: String) {
    ISSUE("issue"),
    PULL_REQUEST("pull-request")
}

/**
 * The user or team that was mentioned on GitHub.
 *
 * @property fullName The full name of the target being searched for,
 *   used to identify it on GitHub.
 * @property tag The GitHub tag of the user or team.
 * @property type The type of GitHub user or team mentioned.
 */
private class MentionTarget private constructor(
    val fullName: String,
    val tag: String,
    val type: MentionType
) {
    constructor(user: Username) : this(user.value, user.tag, MentionType.PERSON)
    constructor(team: Team) : this(team.fullName, team.tag, MentionType.TEAM)
}

/**
 * The type of GitHub entity mentioned.
 */
private enum class MentionType {
    /**
     * Mentioned a specific person using their username.
     */
    PERSON,

    /**
     * Mentioned a specific team using its full name.
     */
    TEAM
}

/**
 * Creates a search request builder.
 */
private fun HttpClient.search(url: String): SearchRequestBuilder =
    SearchRequestBuilder(this, url)

/**
 * A builder for creating and sending requests to search for issues and pull requests
 * where the user is involved or team is mentioned on GitHub.
 *
 * If [target] is [user][MentionType.PERSON] then created search request
 * will find issues and pull requests that were either created by a certain user,
 * assigned to that user, mention that user, or were commented on by that user.
 * Searching for mentions using [mentions:username](https://shorturl.at/zQzGL) is insufficient,
 * as it only captures mentions in issue comments, missing those in pull request reviews
 * and review comments. Instead, all issues and pull requests where the user was involved
 * are retrieved. Among them, mentions will then need to be selected.
 *
 * The `team:full-name` filter is used to search for team mentions.
 *
 * @property client The HTTP client on behalf of which requests is made.
 * @property url The GitHub REST API search endpoint.
 * @see <a href="https://shorturl.at/6z3UB">
 *     Search by a user that's involved in an issue or pull request</a>
 * @see <a href="https://shorturl.at/bN39m">Search by team mention</a>
 */
private class SearchRequestBuilder(private val client: HttpClient, private val url: String) {
    /**
     * The user or team that was mentioned on GitHub.
     */
    private var target: MentionTarget? = null

    /**
     * The type of the searched item.
     */
    private var itemType: ItemType? = null

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
     * The page number of the results to fetch.
     */
    private var page: Int? = null

    /**
     * Sets the user or team that was mentioned on GitHub.
     */
    fun of(target: MentionTarget): SearchRequestBuilder {
        this.target = target
        return this
    }

    /**
     * Sets the type of the searched item.
     */
    fun by(itemType: ItemType): SearchRequestBuilder {
        this.itemType = itemType
        return this
    }

    /**
     * Sets the time after which GitHub items containing the searched mentions
     * should have been updated.
     */
    fun by(updatedAfter: Timestamp): SearchRequestBuilder {
        this.updatedAfter = updatedAfter
        return this
    }

    /**
     * Sets the user authentication token on GitHub
     */
    fun with(token: PersonalAccessToken): SearchRequestBuilder {
        this.token = token
        return this
    }

    /**
     * Sets the page number of the results to fetch.
     */
    fun onPage(page: Int): SearchRequestBuilder {
        this.page = page
        return this
    }

    /**
     * Creates and sends request with specified data.
     *
     * @throws IllegalArgumentException some request data is not specified.
     */
    suspend fun get(): HttpResponse {
        checkNotNull(target) {
            "It's not clear whose mentions to search for. " +
                    "Provide the target whose mentions are to be retrieved."
        }
        checkNotNull(itemType) { "The type of the searched item is not specified." }
        checkNotNull(updatedAfter) {
            "The time after which GitHub items containing the searched mentions " +
                    "should have been updated is not specified."
        }
        checkNotNull(token) {
            "The user authentication token on GitHub is not specified."
        }
        checkNotNull(page) { "The page number of the results to fetch is not specified." }

        return client.get(url) {
            url {
                parameters.append("q", query())
                parameters.append("per_page", perPage.toString())
                parameters.append("page", page!!.toString())
                parameters.append("sort", "updated")
                parameters.append("order", "desc")
            }
            configureHeaders(token!!)
        }
    }

    /**
     * Creates a query to search for mentions based on the type of entity mentioned.
     */
    private fun query(): String {
        val filter = when (target!!.type) {
            MentionType.PERSON -> "involves:${target!!.fullName}"
            MentionType.TEAM -> "team:${target!!.fullName}"
        }
        return "is:${itemType!!.value} $filter " +
                "updated:>${Timestamps.toString(updatedAfter)}"
    }
}

/**
 * Creates a builder for request to obtain mentions on a specific issue or pull request.
 */
private fun HttpClient.findMentionsOn(repo: Repo, number: Int, itemType: ItemType):
        MentionsOnIssueOrPullRequestsBuilder =
    MentionsOnIssueOrPullRequestsBuilder(this, repo, number, itemType)

/**
 * A builder for creating and sending request to obtain mentions
 * on particular issue or pull request on GitHub.
 *
 * In an issue, a user or team can only be mentioned in issue comments. In a pull request,
 * a user or team can be mentioned in issue comments, reviews, and review comments.
 *
 * @property client The HTTP client on behalf of which requests is made.
 * @property repo The repository containing this issue or pull request.
 * @property number The number of this issue or pull request in the repository.
 * @property itemType The GitHub item type.
 */
private class MentionsOnIssueOrPullRequestsBuilder(
    private val client: HttpClient,
    private val repo: Repo,
    private val number: Int,
    private val itemType: ItemType
) : Logging {
    /**
     * The user or team that was mentioned on GitHub.
     */
    private var target: MentionTarget? = null

    /**
     * The user authentication token on GitHub.
     */
    private var token: PersonalAccessToken? = null

    /**
     * The time after which GitHub items containing the searched mentions
     * should have been updated.
     */
    private var updatedAfter: Timestamp? = null

    /**
     * The string to be used as the title for the found mentions.
     *
     * It is recommended to use the title of the GitHub item where
     * the mentions are being searched.
     */
    private var title: String? = null

    /**
     * Sets the user or team that was mentioned on GitHub.
     */
    fun of(target: MentionTarget): MentionsOnIssueOrPullRequestsBuilder {
        this.target = target
        return this
    }

    /**
     * Sets the user authentication token on GitHub.
     */
    fun with(token: PersonalAccessToken): MentionsOnIssueOrPullRequestsBuilder {
        this.token = token
        return this
    }

    /**
     * Sets the time after which GitHub items containing the searched mentions
     * should have been updated.
     */
    fun by(updatedAfter: Timestamp): MentionsOnIssueOrPullRequestsBuilder {
        this.updatedAfter = updatedAfter
        return this
    }

    /**
     * Sets the string to be used as the title for the found mentions.
     */
    fun setTitle(title: String): MentionsOnIssueOrPullRequestsBuilder {
        this.title = title
        return this
    }

    /**
     * Returns the user or team mentions on pull request or issue.
     *
     * @throws IllegalArgumentException some request data is not specified.
     * @throws CannotObtainMentionsException if retrieving data from GitHub fails.
     */
    @Throws(CannotObtainMentionsException::class)
    fun get(): Set<Mention> {
        checkNotNull(target) {
            "It's not clear whose mentions to search for. " +
                    "Provide the target whose mentions are to be retrieved."
        }
        checkNotNull(token) { "The the user authentication token on GitHub is not specified." }
        checkNotNull(updatedAfter) {
            "The time after which GitHub items containing the searched mentions " +
                    "should have been updated is not specified."
        }
        checkNotNull(title) { "The title for the found mentions is not specified." }
        return comments(issueCommentsUrl) + when (itemType) {
            ItemType.ISSUE -> emptySet()
            ItemType.PULL_REQUEST -> comments(reviewCommentsUrl) + reviews()
        }
    }

    /**
     * Requests issue or review comments from GitHub,
     * filters for those containing user or team mentions, and parses them.
     */
    @Throws(CannotObtainMentionsException::class)
    private fun comments(url: String): Set<Mention> =
        obtainByUrl(url, CommentsResponse::class::parseJson)
            .itemList
            .filter { comment -> comment.body.contains(target!!.tag) }
            .map { comment -> Mention::class.from(comment, title!!) }
            .filter { comments -> comments.whenMentioned > updatedAfter!! }
            .toSet()

    /**
     * Requests pull request reviews from GitHub,
     * filters for those containing user or team mentions, and parses them.
     */
    @Throws(CannotObtainMentionsException::class)
    private fun reviews(): Set<Mention> =
        obtainByUrl(reviewsUrl, ReviewsResponse::class::parseJson)
            .itemList
            .filter { review -> review.body.contains(target!!.tag) }
            .map { review -> Mention::class.from(review, title!!) }
            .filter { review -> review.whenMentioned > updatedAfter!! }
            .toSet()

    /**
     * The GitHub REST API endpoint for retrieving all comments
     * for the specified issues or pull request.
     *
     * @see <a href="https://shorturl.at/kHNu0">List issue comments</a>
     */
    private val issueCommentsUrl: String
        get() = "https://api.github.com/repos/${repo.owner}/${repo.name}/issues/$number/comments"

    /**
     * The GitHub REST API endpoint for retrieving all review comments
     * for the specified pull request.
     *
     * @see <a href="https://shorturl.at/qI29x">List review comments on a pull request</a>
     */
    private val reviewCommentsUrl: String
        get() = "https://api.github.com/repos/${repo.owner}/${repo.name}/pulls/$number/comments"

    /**
     * The GitHub REST API endpoint for retrieving all reviews for a specified pull request.
     *
     * @see <a href="https://shorturl.at/Q50Bu">List reviews for a pull request</a>
     */
    private val reviewsUrl: String
        get() = "https://api.github.com/repos/${repo.owner}/${repo.name}/pulls/$number/reviews"

    /**
     * Sends a request to GitHub REST API to obtain comments or reviews on their URL.
     */
    @Throws(CannotObtainMentionsException::class)
    private fun <R> obtainByUrl(url: String, parser: (String) -> R): R =
        runBlocking {
            val response = client.get(url) {
                configureHeaders(token!!)
            }
            val json = response.body<String>()
            if (response.status != HttpStatusCode.OK) {
                _error().log(
                    "An error occurred while retrieving GitHub items, and GitHub responded " +
                            "with: ${json.replace("(\\r\\n|\\r|\\n)".toRegex(), "")}."
                )
                throw CannotObtainMentionsException(response.status.value)
            }
            // The received JSON contains only an array, but Protobuf JSON Parser
            // cannot process it. So the array is converted to JSON, where the result
            // is just the value of the `item` field.
            parser("{ item: $json }")
        }
}

/**
 * Configures headers for an HTTP request to the GitHub REST API.
 *
 * @see <a href="https://shorturl.at/sPHYj">Authenticating to the GitHub REST API</a>
 */
private fun HttpMessageBuilder.configureHeaders(token: PersonalAccessToken): HeadersBuilder =
    headers.apply {
        append("Authorization", "Bearer ${token.value}")
        append("Accept", "application/vnd.github+json")
    }
