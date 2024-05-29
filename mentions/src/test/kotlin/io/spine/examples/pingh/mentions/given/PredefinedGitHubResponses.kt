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
import io.spine.examples.pingh.github.PersonalAccessToken
import io.spine.examples.pingh.github.Username
import io.spine.examples.pingh.mentions.GitHubClient
import io.spine.examples.pingh.mentions.GitHubClientService
import io.spine.examples.pingh.mentions.parseJson
import java.lang.Thread.sleep

/**
 * Implementation of [GitHubClientService] that fetches mentions
 * from a JSON file in the resource folder.
 *
 * Uses exclusively for testing [GitHubClient] behavior.
 */
public class PredefinedGitHubResponses : GitHubClientService {

    /**
     * Indicates whether to freeze the execution of the [fetchMentions] method.
     *
     * If `true`, the method will be executed indefinitely, if `false`,
     * it will terminate without problems. The value can be changed during execution.
     */
    private var isFrozen = false

    /**
     * Returns set of [Mention]s retrieved from a JSON file in the resource folder.
     */
    public override fun fetchMentions(
        username: Username,
        token: PersonalAccessToken
    ): Set<Mention> {
        val jsonFile = this::class.java.getResource("github_response.json")
        checkNotNull(jsonFile)
        val json = jsonFile.readText(Charsets.UTF_8)
        val mentions = parseJson(json)
        waitWhileServiceIsFrozen()
        return mentions
    }

    /**
     * Waits for the service to be unfrozen.
     */
    private fun waitWhileServiceIsFrozen() {
        while (isFrozen) {
            sleep(100)
        }
    }

    /**
     * Marks the service as frozen.
     *
     * The process of fetching mentions from GitHub will not be completed until the service
     * is unfrozen by calling the [unfreeze] method.
     */
    public fun freeze() {
        isFrozen = true
    }

    /**
     * Marks the service as unfrozen, i.e. the process of fetching mentions
     * from GitHub can be completed.
     */
    public fun unfreeze() {
        isFrozen = false
    }
}
