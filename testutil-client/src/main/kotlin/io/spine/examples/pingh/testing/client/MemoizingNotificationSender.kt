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

package io.spine.examples.pingh.testing.client

import com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly
import io.kotest.matchers.shouldBe
import io.spine.examples.pingh.client.MentionDetails
import io.spine.examples.pingh.client.UserAlert
import java.lang.AssertionError
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Memorizes the number of notifications that should be sent.
 *
 * Does not send any notifications. Use only for tests.
 */
internal class MemoizingUserAlert : UserAlert {

    /**
     * The number of notifications that should be sent.
     */
    private var notificationsCount = 0

    /**
     * Adds a notification to [total number][notificationsCount] that should be sent.
     */
    override fun notifySessionExpired() {
        notificationsCount++
    }

    /**
     * Adds a notification to [total number][notificationsCount] that should be sent.
     */
    override fun notifyMention(mention: MentionDetails) {
        notificationsCount++
    }

    /**
     * Obtains the count of notifications that should be sent.
     */
    internal fun notificationsCount(): Int = notificationsCount
}

/**
 * Checks for notifications sent by the `UserAlert`.
 *
 * The server may not send the notification immediately,
 * so the check will be repeated at intervals until it succeeds.
 */
public class DelayedNotificationAssertion internal constructor(
    private val alert: MemoizingUserAlert
) {
    private companion object {
        /**
         * The waiting period before performing the next check after an unsuccessful attempt.
         */
        private val intervalBetweenChecks = 100.milliseconds

        /**
         * The duration within which a successful check must occur.
         */
        private val maxExpectation = 5.seconds
    }

    /**
     * Fails if the notification count does not match the specified size.
     */
    public fun hasSize(expected: Int) {
        awaitFact { alert.notificationsCount() shouldBe expected }
    }

    private fun awaitFact(assertion: () -> Unit) {
        var error: AssertionError? = null
        val timer = CompletableFuture.supplyAsync { delay(maxExpectation) }
        while (!timer.isDone) {
            try {
                assertion()
                return
            } catch (e: AssertionError) {
                error = e
            } finally {
                delay(intervalBetweenChecks)
            }
        }
        throw error ?: AssertionError("No checks have been made.")
    }
}

private fun delay(duration: Duration) {
    sleepUninterruptibly(duration.inWholeMilliseconds, TimeUnit.MILLISECONDS)
}
