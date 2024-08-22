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

package io.spine.examples.pingh.mentions.given

import com.google.protobuf.util.Timestamps
import io.kotest.assertions.fail
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.spine.examples.pingh.github.Mention
import io.spine.examples.pingh.github.NodeId
import io.spine.examples.pingh.github.PersonalAccessToken
import io.spine.examples.pingh.github.User
import io.spine.examples.pingh.github.buildBy
import io.spine.examples.pingh.github.of
import io.spine.examples.pingh.testing.mentions.given.PredefinedGitHubResponses
import io.spine.net.Url

/**
 * Intercepts requests to GitHub and returns prepared responses from JSON
 * that contain mentions of the user.
 */
internal fun mockEngineThatContainsMentions(token: PersonalAccessToken): HttpClientEngine =
    MockEngine { request ->
        if (request.headers["Authorization"] != "Bearer ${token.value}") {
            fail("Authentication failed.")
        }

        if (request.url.pathSegments[2] == "issues") {
            if (request.url.parameters["q"]!!.startsWith("is:pull-request")) {
                val body = loadJson("prs-search-response.json")
                return@MockEngine respond(body)
            }
            if (request.url.parameters["q"]!!.startsWith("is:issue")) {
                val body = loadJson("issues-search-response.json")
                return@MockEngine respond(body)
            }
        }

        if (request.url.pathSegments.size == 7 && request.url.pathSegments[6] == "comments") {
            if (request.url.pathSegments[5] == "3") {
                val body = loadJson("comments-under-pr-response.json")
                return@MockEngine respond(body)
            }
            if (request.url.pathSegments[5] == "8") {
                val body = loadJson("comments-under-issue-response.json")
                return@MockEngine respond(body)
            }
        }

        fail("Unexpected request.")
    }

/**
 * Returns the set of `Mentions` that must be fetched from GitHub.
 */
internal fun expectedMentions(): Set<Mention> =
    setOf(
        Mention.newBuilder()
            .setId("IC_kwDOL2L5hc5-0lTv")
            .setAuthor("armiol", "https://avatars.githubusercontent.com/u/82468?v=4")
            .setTitle("Request updates for assigned issues without activity")
            .setWhenMentioned("2024-05-23T17:38:40Z")
            .setUrl("https://github.com/spine-examples/Pingh/pull/8#issuecomment-2127713519")
            .vBuild(),
        Mention.newBuilder()
            .setId("IC_kwDOL2L5hc5-0lTv")
            .setAuthor("armiol", "https://avatars.githubusercontent.com/u/82468?v=4")
            .setTitle("Implement user session flow")
            .setWhenMentioned("2024-05-23T17:38:40Z")
            .setUrl("https://github.com/spine-examples/Pingh/pull/8#issuecomment-2127713519")
            .vBuild(),
        Mention.newBuilder()
            .setId("PR_kwDOL2L5hc5vY_JP")
            .setAuthor("MykytaPimonovTD", "https://avatars.githubusercontent.com/u/160486193?v=4")
            .setTitle("Implement user session flow")
            .setWhenMentioned("2024-05-14T12:12:02Z")
            .setUrl("https://github.com/spine-examples/Pingh/pull/3")
            .vBuild(),
    )

private fun Mention.Builder.setId(value: String): Mention.Builder =
    this.setId(NodeId::class.buildBy(value))

private fun Mention.Builder.setAuthor(username: String, avatarUrl: String): Mention.Builder =
    this.setAuthor(User::class.buildBy(username, avatarUrl))

private fun Mention.Builder.setWhenMentioned(value: String): Mention.Builder =
    this.setWhenMentioned(Timestamps.parse(value))

private fun Mention.Builder.setUrl(value: String): Mention.Builder =
    this.setUrl(Url::class.of(value))

/**
 * Intercepts requests to GitHub and returns an error response
 * with HTTP status `422 UnprocessableEntity`.
 */
internal fun mockEngineThatFailsAllRequest(token: PersonalAccessToken): HttpClientEngine =
    sendSameResponseToSearchingRequests(
        "error-issues-prs-search-response.json",
        token,
        HttpStatusCode.UnprocessableEntity
    )

/**
 * Intercepts requests to GitHub and returns prepared responses from JSON
 * that do not contain mentions of the user.
 */
internal fun mockEngineThatDoesNotContainMentions(token: PersonalAccessToken): HttpClientEngine =
    sendSameResponseToSearchingRequests(
        "empty-issues-prs-search-response.json",
        token,
        HttpStatusCode.OK
    )

/**
 * Creates  `MockEngine` that returns the same JSON response for all requests to search
 * issues and pull requests. Does not process comment requests.
 */
private fun sendSameResponseToSearchingRequests(
    jsonFileName: String,
    token: PersonalAccessToken,
    responseStatusCode: HttpStatusCode
): MockEngine =
    MockEngine { request ->
        if (request.headers["Authorization"] != "Bearer ${token.value}") {
            fail("Authentication failed.")
        }

        if (request.url.pathSegments[2] == "issues") {
            val q = request.url.parameters["q"]!!
            if (q.startsWith("is:issue") || q.startsWith("is:pull-request")) {
                val body = loadJson(jsonFileName)
                return@MockEngine respond(body, responseStatusCode)
            }
        }

        fail("Unexpected request.")
    }

/**
 * Loads JSON by its name and returns the contents of the file.
 */
private fun loadJson(name: String): String {
    val jsonFile = PredefinedGitHubResponses::class.java.getResource("/github-responses/$name")
    checkNotNull(jsonFile)
    return jsonFile.readText(Charsets.UTF_8)
}
