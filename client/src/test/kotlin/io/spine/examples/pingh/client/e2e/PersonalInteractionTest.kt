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

import com.google.protobuf.Duration
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.spine.examples.pingh.client.e2e.given.expectedMentionsList
import io.spine.examples.pingh.client.e2e.given.randomUnread
import io.spine.examples.pingh.client.e2e.given.updateStatusById
import io.spine.examples.pingh.github.Username
import io.spine.examples.pingh.github.buildBy
import io.spine.examples.pingh.mentions.MentionId
import io.spine.examples.pingh.mentions.MentionStatus
import io.spine.examples.pingh.mentions.MentionView
import io.spine.protobuf.Durations2.hours
import io.spine.protobuf.Durations2.milliseconds
import java.lang.Thread.sleep
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * End-to-end test to checks client-server interaction.
 */
public class PersonalInteractionTest : IntegrationTest() {

    private val username = Username::class.buildBy("MykytaPimonovTD")
    private lateinit var actual: List<MentionView>
    private lateinit var expected: List<MentionView>

    @BeforeEach
    public fun logInAndUpdateMentions() {
        client().logIn(username)
        client().updateMentions()
        actual = client().findUserMentions()
        expected = expectedMentionsList(username)
        actual shouldBe expected
    }

    /**
     * End-to-end test that describes such a scenario:
     *
     * 1. The user logs in to the Pingh app.
     * 2. The user updates their mentions from GitHub.
     * 3. The user changes the status of a single mention to snoozing.
     * 4. The user reads one mention.
     */
    @Test
    public fun `the user should log in, update mentions, and change their statuses`() {
        snoozeRandomMention()
        actual shouldBe expected
        readRandomMention()
        actual shouldBe expected
    }

    /**
     * End-to-end test that describes such a scenario:
     *
     * 1. The user logs in to the Pingh app.
     * 2. The user updates their mentions from GitHub.
     * 3. The user reads two random mentions.
     * 4. The user snoozes a random mention for 100 milliseconds.
     * 5. The user waits until the snooze time is over.
     * 6. The user checks that the snoozed mention is already unsnoozed.
     */
    @Test
    public fun `the user should snooze the mention and wait until the snooze time is over`() {
        readRandomMention()
        actual shouldBe expected
        readRandomMention()
        actual shouldBe expected
        val snoozedMentionId = snoozeRandomMention(milliseconds(100))
        actual shouldBe expected
        sleep(300)
        actual = client().findUserMentions()
        expected = expected.updateStatusById(snoozedMentionId, MentionStatus.UNREAD)
        actual shouldBe expected
    }

    /**
     * End-to-end test that describes such a scenario:
     *
     * 1. The user logs in to the Pingh app.
     * 2. The user updates their mentions from GitHub.
     * 3. The user logs out of the Pingh app.
     * 4. The user tries to get mentions but catches the exception.
     * 5. The user logs in again.
     * 6. The user gets mentions that were updated in the first session.
     */
    @Test
    public fun `the user should log in, log out, log in again, and then gets mentions`() {
        client().logOut()
        shouldThrow<IllegalStateException> {
            client().findUserMentions()
        }
        client().logIn(username)
        actual = client().findUserMentions()
        actual shouldBe expected
    }

    private fun readRandomMention(): MentionId {
        val mention = actual.randomUnread()
        client().readMention(mention.id)
        actual = client().findUserMentions()
        expected = expected.updateStatusById(mention.id, MentionStatus.READ)
        return mention.id
    }

    private fun snoozeRandomMention(snoozeTime: Duration = hours(2)): MentionId {
        val mention = actual.randomUnread()
        client().snoozeMention(mention.id, snoozeTime)
        actual = client().findUserMentions()
        expected = expected.updateStatusById(mention.id, MentionStatus.SNOOZED)
        return mention.id
    }
}
