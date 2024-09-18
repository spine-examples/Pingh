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

package io.spine.examples.pingh.testing.mentions.given

import io.spine.examples.pingh.github.Mention
import io.spine.examples.pingh.github.from
import io.spine.examples.pingh.github.rest.CommentsResponse
import io.spine.examples.pingh.github.rest.IssuesAndPullRequestsSearchResult
import io.spine.examples.pingh.mentions.parseJson

/**
 * Returns the set of mentions that [PredefinedGitHubSearchResponses] returns
 * on successful execution.
 */
public fun predefinedMentionsSet(): Set<Mention> =
    loadMentionsInPr() + loadMentionsInCommentsUnderPr()

/**
 * Loads mentions from the predefined JSON file.
 *
 * The predefined response loaded corresponds to the exact response
 * returned by the GitHub API when requesting for mentions in a pull request.
 */
private fun loadMentionsInPr(): Set<Mention> {
    val jsonFile = PredefinedGitHubSearchResponses::class.java
        .getResource("/github-responses/prs-search-response.json")
    checkNotNull(jsonFile)
    val json = jsonFile.readText(Charsets.UTF_8)
    return IssuesAndPullRequestsSearchResult::class.parseJson(json)
        .itemList
        .map { fragment -> Mention::class.from(fragment) }
        .toSet()
}

/**
 * Loads mentions from the predefined JSON file.
 *
 * The predefined response loaded corresponds to the exact response
 * returned by the GitHub API when requesting for mentions in comments under a pull request.
 */
private fun loadMentionsInCommentsUnderPr(): Set<Mention> {
    val jsonFile = PredefinedGitHubSearchResponses::class.java
        .getResource("/github-responses/comments-under-pr-response.json")
    checkNotNull(jsonFile)
    val json = jsonFile.readText(Charsets.UTF_8)
    // The received JSON contains only an array, but Protobuf JSON Parser
    // cannot process it. So the array is converted to JSON, where the result
    // is just the value of the `item` field.
    return CommentsResponse::class.parseJson("{ item: $json }")
        .itemList
        .map { fragment -> Mention::class.from(fragment, "Comment") }
        .toSet()
}
