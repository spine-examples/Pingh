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

import com.google.protobuf.Timestamp
import io.spine.examples.pingh.github.Mention
import io.spine.examples.pingh.github.PersonalAccessToken
import io.spine.examples.pingh.github.Team
import io.spine.examples.pingh.github.Username
import kotlin.jvm.Throws

/**
 * Allows to access GitHub Search API.
 */
public interface GitHubSearch {

    /**
     * Searches for `Mention`s of the user made by others on GitHub.
     *
     * Mentions are searched by the name of the mentioned user. Mentions must be within items
     * updated between the `updateAfter` time and the present. The `PersonalAccessToken` is used
     * to access the GitHub API.
     *
     * @param username The name of the user whose mentions are to be fetched.
     * @param token The `PersonalAccessToken` to access user's private repositories.
     * @param updatedAfter The time after which GitHub items containing the searched mentions
     *   should have been updated.
     * @param limit The maximum number of recent mentions to return.
     */
    @Throws(CannotObtainMentionsException::class)
    public fun searchMentions(
        username: Username,
        token: PersonalAccessToken,
        updatedAfter: Timestamp = Timestamp.getDefaultInstance(),
        limit: Int? = null
    ): Set<Mention>

    /**
     * Searches for `Mention`s of the team on GitHub.
     *
     * @param team The team whose mention is being searched for.
     * @param token The `PersonalAccessToken` to access team mentions.
     * @param updatedAfter The time after which GitHub items containing the searched mentions
     *   should have been updated.
     * @param limit The maximum number of recent mentions to return.
     */
    @Throws(CannotObtainMentionsException::class)
    public fun searchMentions(
        team: Team,
        token: PersonalAccessToken,
        updatedAfter: Timestamp = Timestamp.getDefaultInstance(),
        limit: Int? = null
    ): Set<Mention>
}
