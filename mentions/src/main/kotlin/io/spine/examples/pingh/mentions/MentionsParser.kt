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

import com.google.protobuf.Timestamp
import com.google.protobuf.util.JsonFormat
import io.spine.examples.pingh.github.Mention
import io.spine.examples.pingh.github.ResponseOnSearchingIssuesRequest
import io.spine.examples.pingh.github.ResponseOnSearchingIssuesRequestItem
import io.spine.examples.pingh.github.User
import io.spine.examples.pingh.github.UserValue
import io.spine.examples.pingh.github.Username
import io.spine.net.Url
import java.time.Instant

/**
 * Converts a JSON containing a list of GitHub items
 * where a user is mentioned to a list of [Mention].
 */
public class MentionsParser {

    /**
     * Converts JSON to a list of [Mention].
     */
    public fun parseJson(json: String): List<Mention> {
        val responseBuilder = ResponseOnSearchingIssuesRequest.newBuilder()
        JsonFormat.parser()
            .ignoringUnknownFields()
            .merge(json, responseBuilder)
        val response = responseBuilder.vBuild()
        return mapToMention(response.itemList)
    }

    /**
     * Converts list of [ResponseOnSearchingIssuesRequestItem]s to list of [Mention]s.
     */
    private fun mapToMention(gitHubItems: List<ResponseOnSearchingIssuesRequestItem>):
            List<Mention> =
        gitHubItems
            .map { item ->
                Mention.newBuilder()
                    .setId(item.id)
                    .setWhoMentioned(userBy(item.whoCreated))
                    .setTitle(item.title)
                    .setWhenMentioned(timestampBy(item.whenCreated))
                    .setUrl(urlBy(item.htmlUrl))
                    .vBuild()
            }
            .toList()

    private fun urlBy(specValue: String): Url =
        with(Url.newBuilder()) {
            spec = specValue
            vBuild()
        }

    private fun usernameBy(usernameValue: String): Username =
        with(Username.newBuilder()) {
            value = usernameValue
            vBuild()
        }

    private fun userBy(userValue: UserValue): User =
        with(User.newBuilder()) {
            username = usernameBy(userValue.username)
            avatarUrl = urlBy(userValue.avatarUrl)
            vBuild()
        }

    private fun timestampBy(timeValue: String): Timestamp {
        val instant = Instant.parse(timeValue)
        return with(Timestamp.newBuilder()) {
            seconds = instant.epochSecond
            nanos = instant.nano
            build()
        }
    }
}
