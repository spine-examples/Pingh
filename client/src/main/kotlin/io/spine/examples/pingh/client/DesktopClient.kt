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

import com.google.protobuf.Message
import io.grpc.ManagedChannelBuilder
import io.spine.base.CommandMessage
import io.spine.base.EntityState
import io.spine.base.EventMessage
import io.spine.base.EventMessageField
import io.spine.base.Field
import io.spine.client.Client
import io.spine.client.ClientRequest
import io.spine.client.EventFilter.eq
import io.spine.client.Subscription
import io.spine.core.UserId
import kotlin.reflect.KClass

/**
 * Interacts with [Pingh server][io.spine.examples.pingh.server] via gRPC.
 *
 * @param userId user on whose behalf client requests are made.
 */
@Suppress("TooManyFunctions") // The client must contain multiple methods
// to interact with the server, which does not enable Detekt.
internal class DesktopClient(
    address: String,
    port: Int,
    private val userId: UserId
) {
    private val client: Client

    init {
        val channel = ManagedChannelBuilder
            .forAddress(address, port)
            .usePlaintext()
            .build()
        client = Client
            .usingChannel(channel)
            .build()
    }

    /**
     * Sends a command to the server on behalf of the user.
     */
    internal fun send(command: CommandMessage) {
        clientRequest()
            .command(command)
            .postAndForget()
    }

    /**
     * Reads entity state by the specified ID.
     */
    internal fun <I : Message, E : EntityState> readById(
        id: I,
        type: KClass<E>
    ): E? =
        clientRequest()
            .select(type.java)
            .byId(id)
            .run()
            .getOrNull(0)

    /**
     * Observes the outcome of the command.
     *
     * When a success or failure event is emitted, subscriptions are cancelled.
     */
    internal fun <S : EventMessage, F : EventMessage> observeCommandOutcome(
        id: Message,
        successType: KClass<S>,
        onSuccess: (event: S) -> Unit,
        failType: KClass<F>,
        onFail: (event: F) -> Unit
    ) {
        var subscriptionOnFail: Subscription? = null
        val subscriptionOnSuccess = observeEventOnce(id, successType) { event ->
            stopObservation(subscriptionOnFail!!)
            onSuccess(event)
        }
        subscriptionOnFail = observeEventOnce(id, failType) { event ->
            stopObservation(subscriptionOnSuccess)
            onFail(event)
        }
    }

    /**
     * Observes the provided event with the specified ID.
     */
    private fun <E : EventMessage> observeEvent(
        id: Message,
        type: KClass<E>,
        onEmit: (event: E) -> Unit
    ): Subscription =
        clientRequest()
            .subscribeToEvent(type.java)
            .where(eq(EventMessageField(Field.named("id")), id))
            .observe(onEmit)
            .post()

    /**
     * Subscribes to the event of the provided type and cancels itself after
     * the observer has worked.
     */
    internal fun <E : EventMessage> observeEventOnce(
        id: Message,
        type: KClass<E>,
        onEmit: (event: E) -> Unit
    ): Subscription {
        var subscription: Subscription? = null
        subscription = observeEvent(id, type) { event ->
            stopObservation(subscription!!)
            onEmit(event)
        }
        return subscription
    }

    /**
     * Subscribes to the update of the entity with the specified type and ID.
     *
     * The subscription cancels itself after the observer has completed its work.
     */
    internal fun <E : EntityState> observeEntityOnce(
        id: Message,
        type: KClass<E>,
        onUpdated: (entity: E) -> Unit
    ) {
        var subscription: Subscription? = null
        subscription = clientRequest()
            .subscribeTo(type.java)
            .byId(id)
            .observe { entity ->
                stopObservation(subscription!!)
                onUpdated(entity)
            }
            .post()
    }

    /**
     * Stops observation of the provided subscription.
     */
    private fun stopObservation(subscription: Subscription) {
        client.subscriptions()
            .cancel(subscription)
    }

    /**
     * Creates a new instance of the `ClientRequest` on behalf of current client.
     */
    private fun clientRequest(): ClientRequest = client.onBehalfOf(userId)

    /**
     * Closes the client.
     */
    internal fun close() {
        client.close()
    }
}
