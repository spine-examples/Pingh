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

package io.spine.examples.pingh.client.e2e.given

import com.google.protobuf.Message
import io.spine.base.EventMessage
import io.spine.client.Subscription
import io.spine.examples.pingh.client.PinghApplication
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Begins observing the emission of multiple events of the specified [type][E],
 * identified by their [IDs][ids].
 *
 * Subscribes to each event's emission and uses [CompletableFuture] to track
 * the completion of the observation.
 *
 * For the observation to be considered successful, all events specified by
 * their indexes must occur.
 *
 * It's essential to start observing event emissions before sending a command
 * that triggers the expected events; otherwise, you may end up waiting for
 * events that have already been emitted.
 *
 * @param E The type of the observed event.
 *
 * @param ids Event identifiers to be observed.
 */
internal inline fun <reified E : EventMessage> PinghApplication.observeEvents(
    vararg ids: Message
): Observer {
    require(ids.isNotEmpty()) {
        "At least one index must be specified for the observation, but none was provided."
    }
    val future = CompletableFuture<Unit>()
    val subscriptions = mutableSetOf<Subscription>()
    ids.forEach { id ->
        var subscription: Subscription? = null
        subscription = client.observeEventOnce(id, E::class) {
            subscriptions.remove(subscription)
            if (subscriptions.isEmpty()) {
                future.complete(Unit)
            }
        }
        subscriptions.add(subscription)
    }
    return Observer(future)
}

/**
 * Observer for event emissions.
 *
 * Holds a [future] that is marked as `completed` when all
 * expected events have been emitted.
 *
 * @see [PinghApplication.observeEvents]
 */
internal class Observer(private val future: CompletableFuture<Unit>) {
    /**
     * Waits for the specified event emissions to occur.
     *
     * If completion does not occur within 5 seconds of the method call,
     * a [TimeoutException] will be thrown.
     *
     * @see [CompletableFuture.get]
     */
    internal fun waitUntilEmitted() {
        future.get(5, TimeUnit.SECONDS)
    }
}
