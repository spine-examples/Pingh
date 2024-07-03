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
import io.spine.examples.pingh.github.buildFromFragment
import io.spine.examples.pingh.mentions.parseCommentsFromJson
import io.spine.examples.pingh.mentions.parseIssuesAndPullRequestsFromJson

/**
 * Returns the set of mentions that [PredefinedGitHubResponses] returns on successful execution.
 */
public fun predefinedMentionsSet(): Set<Mention> =
    loadMentionsInPr() + loadMentionsInCommentsUnderPr()

/**
 * Loads mentions from the prepared JSON response to a mention search request in pull requests.
 */
private fun loadMentionsInPr(): Set<Mention> {
    val jsonFile = PredefinedGitHubResponses::class.java
        .getResource("/github-responses/prs-search-response.json")
    checkNotNull(jsonFile)
    val json = jsonFile.readText(Charsets.UTF_8)
    return parseIssuesAndPullRequestsFromJson(json)
        .itemList
        .map { fragment -> Mention::class.buildFromFragment(fragment) }
        .toSet()
}

/**
 * Loads mentions from the prepared JSON response to a comments obtain request under pull request.
 */
private fun loadMentionsInCommentsUnderPr(): Set<Mention> {
    val jsonFile = PredefinedGitHubResponses::class.java
        .getResource("/github-responses/comments-under-pr-response.json")
    checkNotNull(jsonFile)
    val json = jsonFile.readText(Charsets.UTF_8)
    // The received JSON contains only an array, but Protobuf JSON Parser
    // cannot process it. So the array is converted to JSON, where the result
    // is just the value of the `item` field.
    return parseCommentsFromJson("{ item: $json }")
        .itemList
        .map { fragment -> Mention::class.buildFromFragment(fragment, "Comment") }
        .toSet()
}
