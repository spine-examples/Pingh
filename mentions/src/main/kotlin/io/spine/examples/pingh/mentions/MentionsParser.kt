/*
 * Copyright 2024, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import com.google.protobuf.util.JsonFormat
import com.google.protobuf.util.Timestamps
import io.spine.examples.pingh.github.Mention
import io.spine.examples.pingh.github.NodeId
import io.spine.examples.pingh.github.User
import io.spine.examples.pingh.github.buildBy
import io.spine.examples.pingh.github.rest.IssueOrPullRequestFragment
import io.spine.examples.pingh.github.rest.IssuesAndPullRequestsSearchResult
import io.spine.net.Url

/**
 * Converts a JSON containing a list of GitHub items
 * where a user is mentioned to a list of [Mention]s.
 */
public class MentionsParser {

    /**
     * Converts JSON to a list of [Mention]s.
     */
    public fun parseJson(json: String): List<Mention> {
        val responseBuilder = IssuesAndPullRequestsSearchResult.newBuilder()
        JsonFormat.parser()
            .ignoringUnknownFields()
            .merge(json, responseBuilder)
        val response = responseBuilder.vBuild()
        return mapToMention(response.itemList)
    }

    /**
     * Converts list of [IssueOrPullRequestFragment]s to list of [Mention]s.
     */
    private fun mapToMention(gitHubItems: List<IssueOrPullRequestFragment>):
            List<Mention> =
        gitHubItems
            .map { fragment ->
                with(Mention.newBuilder()) {
                    id = NodeId::class.buildBy(fragment.nodeId)
                    whoMentioned = User::class.buildBy(
                        fragment.whoCreated.username,
                        fragment.whoCreated.avatarUrl
                    )
                    title = fragment.title
                    whenMentioned = Timestamps.parse(fragment.whenCreated)
                    url = Url::class.buildBy(fragment.htmlUrl)
                    vBuild()
                }
            }
            .toList()
}
