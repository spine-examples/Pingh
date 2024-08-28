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

@file:Suppress("UnusedReceiverParameter" /* Class extensions don't use class as a parameter. */)

package io.spine.examples.pingh.mentions.given

import com.google.protobuf.Timestamp
import io.spine.examples.pingh.github.PersonalAccessToken
import io.spine.examples.pingh.github.Username
import io.spine.examples.pingh.github.of
import io.spine.examples.pingh.mentions.AutoUpdateMentions
import io.spine.examples.pingh.mentions.GitHubClientId
import io.spine.examples.pingh.mentions.buildBy
import io.spine.examples.pingh.mentions.event.GitHubTokenUpdated
import io.spine.examples.pingh.mentions.of
import io.spine.examples.pingh.sessions.event.UserLoggedIn
import io.spine.testing.TestValues.randomString
import java.util.*
import kotlin.reflect.KClass

/**
 * Creates a new `GitHubClientId` with a randomly generated `Username`.
 */
internal fun KClass<GitHubClientId>.generate(): GitHubClientId =
    GitHubClientId::class.of(Username::class.of(randomString()))

/**
 * Creates a new `UserLoggedIn` event with ID of the `GitHubClient`
 * and randomly generated `PersonalAccessToken`.
 */
internal fun KClass<UserLoggedIn>.withId(id: GitHubClientId): UserLoggedIn =
    buildBy(id.username, PersonalAccessToken::class.of(randomString()))

/**
 * Creates a new `GitHubTokenUpdated` event with the passed `GitHubClientId`
 * and randomly generated `PersonalAccessToken`.
 */
internal fun KClass<GitHubTokenUpdated>.withId(id: GitHubClientId): GitHubTokenUpdated =
    buildBy(id, PersonalAccessToken::class.of(randomString()))

/**
 * Creates a new `AutoUpdateMentions` with the passed `GitHubClientId`.
 *
 * The time of the last update request is optional.
 */
internal fun KClass<AutoUpdateMentions>.with(
    id: GitHubClientId,
    whenLastRequested: Timestamp? = null
): AutoUpdateMentions =
    with(AutoUpdateMentions.newBuilder()) {
        this.id = id
        if (!Objects.equals(whenLastRequested, null)) {
            this.whenLastRequested = whenLastRequested
        }
        vBuild()
    }
