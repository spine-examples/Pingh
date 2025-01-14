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

import io.spine.base.Time.currentTime
import io.spine.core.UserId
import io.spine.examples.pingh.clock.event.TimePassed
import io.spine.server.ServerEnvironment
import io.spine.server.integration.ThirdPartyContext
import io.spine.server.transport.TransportFactory

/**
 * Allows to emit events with current time in the Clock bounded context.
 *
 * A [third-party context][ThirdPartyContext] is used for emitting events,
 * and it is initialized with the [transport][TransportFactory] specified in
 * the [server environment][ServerEnvironment]. The context is created upon the first invocation
 * of the [method][triggerTimePassed] that emits events with the current time.
 * Therefore, before emitting, ensure that the `transport` is set in
 * the server environment and matches the one used by the main server.
 */
public abstract class Clock {
    /**
     * The Clock bounded context that is designed to notify the system of the current time.
     */
    private var context: ThirdPartyContext? = null

    /**
     * Emits the `TimePassed` event that contains the current time.
     */
    protected fun triggerTimePassed() {
        if (context == null) {
            context = ThirdPartyContext.singleTenant(contextName)
        }
        val event = TimePassed::class.buildBy(currentTime())
        context!!.emittedEvent(event, actor)
    }

    /**
     * Starts the clock.
     */
    public abstract fun start()

    private companion object {
        /**
         * The name of the Clock bounded context.
         */
        private const val contextName = "Clock"

        /**
         * The system actor notifying about the current time.
         */
        private val actor = UserId.newBuilder()
            .setValue("Pingh-Clock")
            .vBuild()
    }
}
