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
import io.spine.examples.pingh.client.EnterUsername
import io.spine.examples.pingh.client.MentionsFlow
import io.spine.examples.pingh.client.VerifyLogin
import io.spine.examples.pingh.client.e2e.given.expectedMentionsList
import io.spine.examples.pingh.client.e2e.given.observeEvents
import io.spine.examples.pingh.client.e2e.given.randomUnread
import io.spine.examples.pingh.clock.emitTimePassedEvent
import io.spine.examples.pingh.github.Username
import io.spine.examples.pingh.github.of
import io.spine.examples.pingh.mentions.MentionId
import io.spine.examples.pingh.mentions.MentionView
import io.spine.examples.pingh.mentions.event.MentionRead
import io.spine.examples.pingh.mentions.event.MentionSnoozed
import io.spine.examples.pingh.mentions.event.MentionUnsnoozed
import io.spine.examples.pingh.mentions.event.UserMentioned
import io.spine.examples.pingh.testing.client.IntegrationTest
import io.spine.protobuf.Durations2.hours
import io.spine.protobuf.Durations2.milliseconds
import java.lang.Thread.sleep
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * End-to-end test to checks client-server interaction.
 *
 * The Pingh Server is designed according to the Event Sourcing architectural pattern,
 * which inherently provides
 * [eventual consistency](https://en.wikipedia.org/wiki/Eventual_consistency).
 * When executing a command, cannot guarantee the immediate application of changes.
 * Therefore, the tests are frozen until the necessary actions take place on the server.
 * The [CompletableFuture] is used for this purpose.
 */
internal class PersonalInteractionIgTest : IntegrationTest() {

    private val username = Username::class.of("MykytaPimonovTD")
    private lateinit var allMentions: List<MentionView>

    @BeforeEach
    internal fun logInAndUpdateMentions() {
        allMentions = expectedMentionsList(username)
        val ids = allMentions.map { it.id }
        val observer = app().observeEvents<UserMentioned>(*ids.toTypedArray())
        logIn()
        observer.waitUntilEmitted()
    }

    private fun logIn() {
        val future = CompletableFuture<Unit>()
        val loginFlow = app().startLoginFlow()
        (loginFlow.currentStage().value as EnterUsername).requestUserCode(username) {
            enterUserCode()
            (loginFlow.currentStage().value as VerifyLogin).waitForAuthCompletion(
                onSuccess = {
                    future.complete(Unit)
                }
            )
        }
        future.get(2, TimeUnit.SECONDS)
    }

    /**
     * End-to-end test that describes such a scenario:
     *
     * The user:
     *
     * 1. Logs in to the Pingh app.
     * 2. Updates their mentions from GitHub.
     * 3. Snoozes a random mention
     * 4. Reads the snoozed mention.
     */
    @Test
    internal fun `the user should snooze the mention, and then read this mention`() {
        val mentionsFlow = app().startMentionsFlow()
        val snoozedMentionId = mentionsFlow.snoozeRandomMention()
        val observer = app().observeEvents<MentionRead>(snoozedMentionId)
        mentionsFlow.markAsRead(snoozedMentionId)
        observer.waitUntilEmitted()
    }

    /**
     * End-to-end test that describes such a scenario:
     *
     * The user:
     *
     * 1. Logs in to the Pingh app.
     * 2. Updates their mentions from GitHub.
     * 3. Snoozes a random mention for 500 milliseconds.
     * 4. Waits until the snooze time is over.
     * 5. Checks that the snoozed mention is already unsnoozed.
     */
    @Test
    internal fun `the user should snooze the mention and wait until the snooze time is over`() {
        val mentionsFlow = app().startMentionsFlow()
        val snoozedMentionId = mentionsFlow.snoozeRandomMention(milliseconds(500))
        sleep(1000)
        val observer = app().observeEvents<MentionUnsnoozed>(snoozedMentionId)
        emitTimePassedEvent()
        observer.waitUntilEmitted()
    }

    /**
     * End-to-end test that describes such a scenario:
     *
     * The user:
     *
     * 1. Logs in to the Pingh app.
     * 2. Updates their mentions from GitHub.
     * 3. Logs out of the Pingh app.
     * 4. Tries to get mentions but catches the exception.
     * 5. Logs in again.
     *
     * In this test, the arrival of an event in response to a dispatched command is important,
     * so all assertions take place in the client callbacks. The [CompletableFuture] is used
     * to ensure that the test does not end before the asynchronous callback is called.
     */
    @Test
    internal fun `the user should log in, log out, and log in again`() {
        val settingsFlow = app().startSettingsFlow()
        val future = CompletableFuture<Unit>()
        settingsFlow.logOut {
            shouldThrow<IllegalStateException> {
                val mentionsFlow = app().startMentionsFlow()
                mentionsFlow.allMentions()
            }
            logIn()
            future.complete(Unit)
        }
        future.get(5, TimeUnit.SECONDS)
    }

    /**
     * End-to-end test that describes such a scenario:
     *
     * The user:
     *
     * 1. Logs in to the Pingh app.
     * 2. Updates their mentions from GitHub.
     * 3. Obtains notifications of new mentions.
     * 4. Snoozes a random mention for 500 milliseconds.
     * 5. Waits until the snooze time is over.
     * 6. Obtains notification of the mention that has been unsnoozed.
     */
    @Test
    internal fun `notifications about new and unsnoozed mentions should be sent to the user`() {
        assertNotifications().hasSize(allMentions.size)
        val mentionsFlow = app().startMentionsFlow()
        mentionsFlow.snoozeRandomMention(milliseconds(500))
        sleep(1000)
        emitTimePassedEvent()
        assertNotifications().hasSize(allMentions.size + 1)
    }

    private fun MentionsFlow.snoozeRandomMention(snoozeTime: Duration = hours(2)): MentionId {
        val mention = allMentions.randomUnread()
        val observer = app().observeEvents<MentionSnoozed>(mention.id)
        snooze(mention.id, snoozeTime)
        observer.waitUntilEmitted()
        return mention.id
    }
}
