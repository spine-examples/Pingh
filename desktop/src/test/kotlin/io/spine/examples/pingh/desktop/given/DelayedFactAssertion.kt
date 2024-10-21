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

package io.spine.examples.pingh.desktop.given

import java.lang.AssertionError
import java.util.concurrent.CompletableFuture
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Checks whether the specified assertion has been successful within the given time period.
 *
 * Compose uses coroutines to update UI elements, which means that updates
 * may not occur immediately after a state change. Therefore, periodic checking over
 * a specified duration is essential.
 *
 * @param assertion The assertion that must be checked.
 */
internal class DelayedFactAssertion private constructor(private val assertion: () -> Unit) {
    internal companion object {
        /**
         * The waiting period before performing the next check after an unsuccessful attempt.
         */
        private val intervalBetweenChecks = 100.milliseconds

        /**
         * Waits for the delayed fact to occur by repeatedly performing the `assertion`.
         *
         * Throws exception if no attempt was successful in the given `duration`.
         *
         * @param duration The duration within which a successful check must occur.
         * @param assertion The assertion that must be checked.
         */
        internal fun awaitFact(duration: Duration = 5.seconds, assertion: () -> Unit) {
            DelayedFactAssertion(assertion).awaitFact(duration)
        }
    }

    /**
     * The recent error that occurred during checking before the allocated time expired.
     */
    private var error: AssertionError? = null

    /**
     * Performs periodic `assertion` checks.
     *
     * Terminates if a check succeeds. If no successful check occurs before
     * the check `duration` ends, throws an exception.
     */
    private fun awaitFact(duration: Duration) {
        val timer = CompletableFuture.supplyAsync { delay(duration) }
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
        throw error ?: IllegalStateException("No checks have been made.")
    }
}
