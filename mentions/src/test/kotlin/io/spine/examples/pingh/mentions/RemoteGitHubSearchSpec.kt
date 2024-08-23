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

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.spine.examples.pingh.github.PersonalAccessToken
import io.spine.examples.pingh.github.Username
import io.spine.examples.pingh.github.of
import io.spine.examples.pingh.mentions.given.expectedMentions
import io.spine.examples.pingh.mentions.given.mockEngineThatContainsMentions
import io.spine.examples.pingh.mentions.given.mockEngineThatFailsAllRequest
import io.spine.examples.pingh.mentions.given.mockEngineThatDoesNotContainMentions
import io.spine.testing.TestValues.randomString
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`RemoteGitHubSearch` should")
internal class RemoteGitHubSearchSpec {

    private val username = Username::class.of("MykytaPimonovTD")
    private lateinit var token: PersonalAccessToken

    @BeforeEach
    internal fun generateToken() {
        token = PersonalAccessToken::class.of(randomString())
    }

    @Test
    internal fun `fetch mentions from GitHub`() {
        val service = RemoteGitHubSearch(mockEngineThatContainsMentions(token))
        val mentions = service.fetchMentions(username, token)
        val expected = expectedMentions()
        mentions shouldBe expected
    }

    @Test
    internal fun `throw exception if fetching from GitHub failed`() {
        val service = RemoteGitHubSearch(mockEngineThatFailsAllRequest(token))
        val exception = shouldThrow<CannotFetchMentionsFromGitHubException> {
            service.fetchMentions(username, token)
        }
        exception.statusCode() shouldBe 422
    }

    @Test
    internal fun `return empty set if the user has not been mentioned`() {
        val service = RemoteGitHubSearch(mockEngineThatDoesNotContainMentions(token))
        val mentions = service.fetchMentions(username, token)
        mentions.shouldBeEmpty()
    }
}
