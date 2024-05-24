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
import io.spine.examples.pingh.github.IssuesSearchResult
import io.spine.examples.pingh.github.IssuesSearchResultItem
import io.spine.examples.pingh.github.Mention
import io.spine.examples.pingh.github.User
import io.spine.examples.pingh.github.buildBy
import io.spine.net.Url

/**
 * Converts a JSON containing a list of GitHub items
 * where a user is mentioned to a list of [Mention].
 */
public class MentionsParser {

    /**
     * Converts JSON to a list of [Mention].
     */
    public fun parseJson(json: String): List<Mention> {
        val responseBuilder = IssuesSearchResult.newBuilder()
        JsonFormat.parser()
            .ignoringUnknownFields()
            .merge(json, responseBuilder)
        val response = responseBuilder.vBuild()
        return mapToMention(response.itemList)
    }

    /**
     * Converts list of [IssuesSearchResultItem]s to list of [Mention]s.
     */
    private fun mapToMention(gitHubItems: List<IssuesSearchResultItem>):
            List<Mention> =
        gitHubItems
            .map { item ->
                with(Mention.newBuilder()) {
                    id = item.id
                    whoMentioned = User::class.buildBy(
                        item.whoCreated.username,
                        item.whoCreated.avatarUrl
                    )
                    title = item.title
                    whenMentioned = Timestamps.parse(item.whenCreated)
                    url = Url::class.buildBy(item.htmlUrl)
                    vBuild()
                }
            }
            .toList()
}
