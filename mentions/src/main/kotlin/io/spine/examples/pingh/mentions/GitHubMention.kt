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

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

/**
 * GitHub user data.
 */
public data class GitHubUser(
    @SerializedName("login") val username: String,
    @SerializedName("avatar_url") val avatarUrl: String
)

/**
 * GitHub mention data.
 */
public data class GitHubMention(
    val id: Long,
    @SerializedName("user") val whoMentioned: GitHubUser,
    val title: String,
    @SerializedName("created_at") val whenCreated: String,
    @SerializedName("html_url") val url: String
)

/**
 * Data from GitHub's response to a request to search for mentions in issues and pull requests.
 */
private data class GitHubResponse(
    @SerializedName("items") val mentions: List<GitHubMention>
)

private val gson = Gson()

/**
 * Converts JSON to a list of [GitHubMention].
 */
public fun fromJson(json: String): List<GitHubMention> {
    val response = gson.fromJson(json, GitHubResponse::class.java)
    return response.mentions
}
