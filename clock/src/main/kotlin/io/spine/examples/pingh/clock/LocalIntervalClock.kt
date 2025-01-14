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

package io.spine.examples.pingh.clock

import io.spine.logging.Logging
import java.lang.Thread.sleep
import kotlin.time.Duration

/**
 * A clock that continuously emits `TimePassed` events at the specified interval.
 *
 * @property pauseTime The time interval between emitting `TimePassed` events.
 */
public class LocalIntervalClock(private val pauseTime: Duration) : Clock(), Logging {
    /**
     * Whether the clock is currently running.
     *
     * Used to control the [clockThread].
     */
    private var isRunning = false

    /**
     * The clock thread emits a `TimePassed` event after passing each time interval.
     */
    private lateinit var clockThread: Thread

    /**
     * Starts the clock.
     */
    public override fun start() {
        isRunning = true
        clockThread = Thread {
            while (isRunning) {
                sleep(pauseTime.inWholeMilliseconds)
                triggerTimePassed()
                _trace().log("An event with the current time was emitted.")
            }
        }
        clockThread.start()
        _info().log(
            "Local clock started and is sending the current time at $pauseTime intervals."
        )
    }

    /**
     * Stops the clock and waits until [clock thread][clockThread] is shut down.
     */
    public fun stop() {
        isRunning = false
        clockThread.join()
        _info().log("Local clock stopped.")
    }
}
