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

package io.spine.examples.pingh.client

import com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly
import com.google.protobuf.util.Durations
import io.spine.logging.Logging
import java.util.concurrent.CompletableFuture
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.toJavaDuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A strategy for retrying an action with exponentially increasing intervals
 * between attempts following each failure.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Exponential_backoff">
 *     Exponential backoff algorithm</a>
 */
internal class ExponentialBackoffStrategy private constructor(builder: Builder) : Logging {
    /**
     * An action that must be repeated until it succeeds.
     */
    private val retryAction = builder.retryAction!!

    /**
     * A minimum duration for the repeat interval.
     */
    private val minDelay = builder.minDelay!!

    /**
     * A maximum duration for the repeat interval.
     */
    private val maxDelay = builder.maxDelay!!

    /**
     * An exponential delay increase coefficient.
     */
    private var factor = builder.factor!!

    /**
     * A timeframe in which [action][retryAction] attempts must be retried.
     */
    private var limit = builder.limit!!

    /**
     * An action to execute upon the [successful completion][ActionOutcome.Success]
     * of a [repeatable task][retryAction].
     */
    private var onSuccess = builder.onSuccess!!

    /**
     * A time counter that completes when the allotted time for the strategy expires.
     */
    private var countdown: CompletableFuture<Unit>? = null

    /**
     * A job that asynchronously performs an [action][retryAction] with a [delay][currentDelay].
     */
    private var retryJob: Job? = null

    /**
     * A current delay time between attempts to execute the [action][retryAction].
     */
    private var currentDelay: Duration? = null

    /**
     * Starts the execution of the exponential backoff strategy.
     *
     * @see [tryPerforming]
     */
    internal fun start() {
        _debug().log("Starting the exponential backoff strategy.")
        currentDelay = minDelay
        countdown = CompletableFuture.supplyAsync { sleepUninterruptibly(limit.toJavaDuration()) }
        tryPerforming()
    }

    /**
     * Attempts to perform the [action][retryAction].
     *
     * If the `action` completes successfully, the [onSuccess()][onSuccess] method is executed
     * and the strategy is terminated.
     *
     * If the `action` completes with rejection, the strategy ends.
     *
     * If the `action` completes unsuccessfully and the execution time has not expired,
     * the `action` is scheduled for re-execution.
     */
    private fun tryPerforming() {
        if (countdown!!.isDone) {
            _debug().log(
                "The exponential backoff strategy failed " +
                        "due to the expiration of the allotted time."
            )
            return
        }
        val status = retryAction()
        when (status) {
            ActionOutcome.Success -> {
                onSuccess()
                _debug().log("The exponential backoff strategy completed successfully.")
            }

            ActionOutcome.Rejection -> {
                _debug().log(
                    "The exponential backoff strategy failed because " +
                            "the retry action ended with a rejected request."
                )
                return
            }

            ActionOutcome.Failure -> {
                val delay = currentDelay!!
                currentDelay = min(currentDelay!! * factor, maxDelay)
                retryJob = invoke(delay, ::tryPerforming)
                _debug().log(
                    "The exponential backoff strategy finished unsuccessfully. " +
                            "A retry will be attempted in $delay."
                )
            }
        }
    }

    /**
     * Stops all scheduled actions.
     */
    internal fun stop() {
        countdown?.cancel(true)
        retryJob?.cancel()
        _debug().log("Exponential backoff strategy stopped manually.")
    }

    /**
     * Status of the completed task result.
     */
    internal enum class ActionOutcome {
        /**
         * The task was completed successfully.
         */
        Success,

        /**
         * The task was completed with rejection.
         */
        Rejection,

        /**
         * The task execution failed; should be restarted after a certain interval.
         */
        Failure
    }

    internal companion object {
        /**
         * Creates a builder to customize the exponential backoff strategy.
         */
        internal fun builder(): Builder = Builder()
    }

    /**
     * Builder to customize the exponential backoff strategy.
     */
    internal class Builder {
        /**
         * An action that must be repeated until it succeeds.
         */
        internal var retryAction: (() -> ActionOutcome)? = null
            private set

        /**
         * A minimum duration for the repeat interval.
         */
        internal var minDelay: Duration? = null
            private set

        /**
         * A maximum duration for the repeat interval.
         */
        internal var maxDelay: Duration? = null
            private set

        /**
         * An exponential delay increase coefficient.
         */
        internal var factor: Double? = null
            private set

        /**
         * A timeframe in which [action][retryAction] attempts must be retried.
         */
        internal var limit: Duration? = null
            private set

        /**
         * An action to execute upon the [successful completion][ActionOutcome.Success]
         * of a [repeatable task][retryAction].
         */
        internal var onSuccess: (() -> Unit)? = null
            private set

        /**
         * Sets the action that must be repeated until it succeeds.
         *
         * @see [ActionOutcome]
         */
        internal fun perform(retryAction: () -> ActionOutcome): Builder {
            this.retryAction = retryAction
            return this
        }

        /**
         * Sets the minimum repetition interval.
         */
        internal fun withMinDelay(minDelay: com.google.protobuf.Duration): Builder {
            this.minDelay = minDelay.toKotlin()
            return this
        }

        /**
         * Sets the maximum repetition interval.
         */
        internal fun withMaxDelay(maxDelay: com.google.protobuf.Duration): Builder {
            this.maxDelay = maxDelay.toKotlin()
            return this
        }

        /**
         * Sets the exponential delay increase coefficient.
         */
        internal fun withFactor(factor: Double): Builder {
            this.factor = factor
            return this
        }

        /**
         * Sets the timeframe in which [action][retryAction] attempts must be retried.
         */
        internal fun withTimeLimit(limit: com.google.protobuf.Duration): Builder {
            this.limit = limit.toKotlin()
            return this
        }

        /**
         * Sets the action to execute upon the [successful completion][ActionOutcome.Success]
         * of a [repeatable task][retryAction].
         */
        internal fun doOnSuccess(action: () -> Unit): Builder {
            onSuccess = action
            return this
        }

        /**
         * Creates a configured [ExponentialBackoffStrategy].
         *
         * @throws IllegalStateException if some strategy data is missing.
         */
        internal fun build(): ExponentialBackoffStrategy {
            checkNotNull(retryAction) {
                "The action to be performed during the strategy must be defined."
            }
            checkNotNull(minDelay) { "The minimum repetition interval must be specified." }
            checkNotNull(maxDelay) { "The maximum repetition interval must be specified." }
            checkNotNull(factor) { "The exponential delay increase coefficient must be specified." }
            checkNotNull(limit) { "The duration of the strategy must be defined." }
            checkNotNull(onSuccess) {
                "The action to execute upon the successful completion " +
                        "of the strategy must be defined."
            }
            return ExponentialBackoffStrategy(this)
        }
    }
}

/**
 * Asynchronously performs work with a delay.
 */
private fun invoke(delay: Duration, action: () -> Unit): Job =
    CoroutineScope(Dispatchers.Default).launch {
        delay(delay)
        action()
    }

/**
 * Converts this duration to Kotlin duration.
 */
private fun com.google.protobuf.Duration.toKotlin(): Duration =
    Durations.toNanos(this).nanoseconds

/**
 * Returns the minimum duration.
 */
private fun min(d1: Duration, d2: Duration): Duration =
    if (d1.inWholeNanoseconds < d2.inWholeNanoseconds) d1 else d2
