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

package io.spine.examples.pingh.client.e2e

import io.kotest.matchers.shouldBe
import io.spine.examples.pingh.client.e2e.given.expectedMentionsSet
import io.spine.examples.pingh.client.e2e.given.updateStatus
import io.spine.examples.pingh.github.Username
import io.spine.examples.pingh.github.buildBy
import io.spine.examples.pingh.mentions.MentionStatus
import org.junit.jupiter.api.Test

/**
 * End-to-end test that describes such a scenario:
 *
 * 1. The user logs in to the Pingh app.
 * 2. The user updates their mentions from GitHub.
 * 3. The user changes the status of a single mention to snoozing.
 * 4. The user reads one mention.
 */
public class PersonalInteractionTest : IntegrationTest() {

    @Test
    public fun `the user should log in, update mentions and change their statuses`() {
        val username = Username::class.buildBy("MykytaPimonovTD")

        client().logIn(username)
        client().updateMentions()

        var actualMentions = client().findUserMentions()
        var expectedMentions = expectedMentionsSet(username)
        actualMentions shouldBe expectedMentions

        var changedMention = actualMentions.random()
        client().snoozeMention(changedMention.id)
        actualMentions = client().findUserMentions()
        expectedMentions = expectedMentions.updateStatus(changedMention.id, MentionStatus.SNOOZED)
        actualMentions shouldBe expectedMentions

        changedMention = actualMentions.random()
        client().readMention(changedMention.id)
        actualMentions = client().findUserMentions()
        expectedMentions = expectedMentions.updateStatus(changedMention.id, MentionStatus.READ)
        actualMentions shouldBe expectedMentions
    }
}
