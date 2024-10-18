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

import com.google.common.annotations.VisibleForTesting
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.spine.base.EventMessage
import io.spine.core.UserId
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.reflect.KClass

/**
 * A client for interacting with the Pingh server for testing.
 *
 * This client provides the ability to subscribe to events and wait for their emission.
 *
 * @param address The address of the Pingh server.
 * @param port The port on which the Pingh server is running.
 */
@VisibleForTesting
public class TestClient(address: String, port: Int) {
    public companion object {
        /**
         * The default amount of seconds to wait
         * when [closing][ManagedChannel.shutdown] the channel.
         */
        private const val defaultShutdownTimeout = 5L
    }

    /**
     * Channel for the communication with the Pingh server.
     */
    private val channel = ManagedChannelBuilder
        .forAddress(address, port)
        .usePlaintext()
        .build()

    /**
     * Enables interaction with the Pingh server.
     */
    private val client = DesktopClient(
        channel,
        UserId.newBuilder()
            .setValue("test-client")
            .build()
    )

    /**
     * Starts observing the emission of the passed event.
     *
     * Subscribes to an observable event. When this event is emitted,
     * the [future][CompletableFuture] is considered complete.
     *
     * It is crucial to begin observing an event before sending a command
     * that triggers its emission. Otherwise, there may be a wait for an event
     * that has already been emitted.
     *
     * @param E The type of the observable event.
     *
     * @param event The class of the type of the observable event.
     */
    public fun <E : EventMessage> observeEvent(event: KClass<E>): Observer {
        val future = CompletableFuture<Unit>()
        val subscription = client.observeEvent(event) {
            future.complete(Unit)
        }
        return Observer(future) { client.cancel(subscription) }
    }

    /**
     * Closes the client.
     */
    public fun close() {
        client.close()
        channel.shutdown()
            .awaitTermination(defaultShutdownTimeout, TimeUnit.SECONDS)
    }
}

/**
 * An observer for event emission.
 *
 * Contains a `CompletableFuture` that will be marked as completed
 * when the event is emitted.
 */
@VisibleForTesting
public class Observer internal constructor(
    private val future: CompletableFuture<Unit>,
    private val cancel: () -> Unit
) {
    /**
     * Waiting for the event to be emitted.
     *
     * If no emission occurs within the specified time,
     * a [TimeoutException] will be thrown.
     *
     * @param millis The wait time for emission, specified in milliseconds.
     */
    public fun waitUntilDone(millis: Long = 2000) {
        try {
            future.get(millis, TimeUnit.MILLISECONDS)
        } finally {
            cancel()
        }
    }
}
