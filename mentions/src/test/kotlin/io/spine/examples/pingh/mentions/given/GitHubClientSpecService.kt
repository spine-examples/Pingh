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

package io.spine.examples.pingh.mentions.given

import io.spine.examples.pingh.github.Mention
import io.spine.examples.pingh.mentions.MentionsParser
import io.spine.examples.pingh.github.PersonalAccessToken
import io.spine.examples.pingh.github.Username
import io.spine.examples.pingh.mentions.GitHubClient
import io.spine.examples.pingh.mentions.GitHubClientService
import java.lang.Thread.sleep

/**
 * Implementation of [GitHubClientService] that fetches mentions
 * from a JSON file in the resource folder.
 *
 * Uses exclusively for testing [GitHubClient] behavior.
 */
public class GitHubClientSpecService : GitHubClientService {

    /**
     * Parses JSON from the GitHub API response to a list of [Mention]s.
     */
    private val mentionsParser = MentionsParser()

    /**
     * Returns list of [Mention]s retrieved from a JSON file in the resource folder.
     */
    public override fun fetchMentions(
        username: Username,
        token: PersonalAccessToken
    ): List<Mention> {
        val jsonFile = this::class.java.getResource("github_response.json")
        checkNotNull(jsonFile)
        val json = jsonFile.readText(Charsets.UTF_8)
        val mentions = mentionsParser.parseJson(json)
        sleep(1000) // Emulates a delay in fetching data from the GitHub API.
        return mentions
    }
}
