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

import java.lang.Thread.sleep
import kotlin.time.Duration

/**
 * The system clock that continuously emits `TimePassed` events at the specified interval.
 */
public class IntervalClock(
    pauseTime: Duration
) {

    /**
     * Whether the clock is currently running. Used to control the [clockThread].
     */
    private var isRunning = false

    /**
     * The clock thread emits a `TimePassed` event after passing each time interval.
     */
    private val clockThread: Thread = Thread {
        while (isRunning) {
            sleep(pauseTime.inWholeMilliseconds)
            emitTimePassedEvent()
        }
    }

    /**
     * Starts the clock.
     */
    public fun start() {
        isRunning = true
        clockThread.start()
    }

    /**
     * Stops the clock and waits while `clockThread` shutdowns.
     */
    public fun stop() {
        isRunning = false
        clockThread.join()
    }
}
