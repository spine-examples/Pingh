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

import io.kotest.assertions.fail
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.spine.examples.pingh.github.Mention
import io.spine.examples.pingh.github.PersonalAccessToken
import io.spine.json.Json

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
            if (request.url.parameters["q"]!!.startsWith("is:issue")) {
                val body = loadJson("response_to_search_issues.json")
                return@MockEngine respond(body)
            }
            if (request.url.parameters["q"]!!.startsWith("is:pull-request")) {
                val body = loadJson("response_to_search_pull_requests.json")
                return@MockEngine respond(body)
            }
        }

        if (request.url.pathSegments.size == 7 && request.url.pathSegments[6] == "comments") {
            if (request.url.pathSegments[5] == "3") {
                val body = loadJson("response_to_obtain_pull_request_comments.json")
                return@MockEngine respond(body)
            }
            if (request.url.pathSegments[5] == "8") {
                val body = loadJson("response_to_obtain_issue_comments.json")
                return@MockEngine respond(body)
            }
        }

        fail("Unexpected request.")
    }

/**
 * Reads mentions from a prepared JSON and returns their set.
 */
internal fun expectedMentions(): Set<Mention> {
    val json = loadJson("expected_github_client_service_result.json")
    return Json.fromJson(json, ExpectedMentionList::class.java)
        .mentionList
        .toSet()
}

/**
 * Intercepts requests to GitHub and returns an error response with HTTP status code 422.
 */
internal fun mockEngineThatFailsAllRequest(token: PersonalAccessToken): HttpClientEngine =
    sendSameResponseToSearchingRequests(
        "error_response_to_search_issues_and_pull_requests.json",
        token,
        HttpStatusCode.UnprocessableEntity
    )

/**
 * Intercepts requests to GitHub and returns prepared responses from JSON
 * that do not contain mentions of the user.
 */
internal fun mockEngineThatDoesNotContainMentions(token: PersonalAccessToken): HttpClientEngine =
    sendSameResponseToSearchingRequests(
        "empty_response_to_search_issues_and_pull_requests.json",
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
    val jsonFile = PredefinedGitHubResponses::class.java.getResource(name)
    checkNotNull(jsonFile)
    return jsonFile.readText(Charsets.UTF_8)
}
