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

import com.google.protobuf.GeneratedMessageV3
import com.google.protobuf.util.JsonFormat
import io.spine.examples.pingh.github.rest.CommentsGetResult
import io.spine.examples.pingh.github.rest.IssuesAndPullRequestsSearchResult
import io.spine.protobuf.ValidatingBuilder

/**
 * Parses `IssuesAndPullRequestsSearchResult` from the JSON.
 */
public fun parseIssuesAndPullRequestsFromJson(json: String): IssuesAndPullRequestsSearchResult =
    parseJson(json, IssuesAndPullRequestsSearchResult.newBuilder())

/**
 * Parses `CommentsGetResult` from the JSON.
 */
public fun parseCommentsFromJson(json: String): CommentsGetResult =
    parseJson(json, CommentsGetResult.newBuilder())

/**
 * Parses a message from JSON.
 */
private fun <M : GeneratedMessageV3, B : ValidatingBuilder<M>> parseJson(
    json: String,
    builder: B
): M {
    JsonFormat.parser()
        .ignoringUnknownFields()
        .merge(json, builder)
    return builder.vBuild()
}