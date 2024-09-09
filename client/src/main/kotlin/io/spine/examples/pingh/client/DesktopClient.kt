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
import io.grpc.ManagedChannel
import io.spine.base.CommandMessage
import io.spine.base.EntityState
import io.spine.base.EventMessage
import io.spine.base.EventMessageField
import io.spine.base.Field
import io.spine.client.Client
import io.spine.client.ClientRequest
import io.spine.client.EventFilter
import io.spine.client.EventFilter.eq
import io.spine.client.Subscription
import io.spine.core.UserId
import kotlin.reflect.KClass

/**
 * Interacts with [Pingh server][io.spine.examples.pingh.server] via gRPC.
 *
 * @param channel The channel for the communication with the Pingh server.
 * @property userId User on whose behalf client requests are made.
 */
@Suppress("TooManyFunctions" /* Many functions are required to interact with the server. */)
internal class DesktopClient(
    channel: ManagedChannel,
    private val userId: UserId? = null
) {
    /**
     * Gateway for backend services that handles commands, queries, and subscriptions.
     */
    private val client = Client
        .usingChannel(channel)
        .build()

    /**
     * Sends a command to the server on behalf of the user.
     *
     * @param command The command to be sent.
     */
    internal fun send(command: CommandMessage) {
        clientRequest()
            .command(command)
            .postAndForget()
    }

    /**
     * Reads the entity state by the specified ID.
     *
     * @param I The type of ID of the entity being read.
     * @param E The type of the entity being read.
     *
     * @param id The ID of the entity state.
     * @param type The class of the type of the entity state.
     * @return The entity state if it exists, or `null` otherwise.
     */
    internal fun <I : Message, E : EntityState> readById(id: I, type: KClass<E>): E? =
        clientRequest()
            .select(type.java)
            .byId(id)
            .run()
            .getOrNull(0)

    /**
     * Observes both events until one is emitted.
     *
     * When either the first or second event is emitted, all subscriptions are cancelled.
     *
     * @param F The type of the first observed event.
     * @param S The type of the second observed event.
     *
     * @param first The information on the observation of the first event.
     * @param second The information on the observation of the second event.
     */
    internal fun <F : EventMessage, S : EventMessage> observeEither(
        first: EventObserver<F>,
        second: EventObserver<S>
    ) {
        var subscriptionOnSecond: Subscription? = null
        val subscriptionOnFirst = observeEventOnce(first.id, first.type) { event ->
            stopObservation(subscriptionOnSecond!!)
            first.callback(event)
        }
        subscriptionOnSecond = observeEventOnce(second.id, second.type) { event ->
            stopObservation(subscriptionOnFirst)
            second.callback(event)
        }
    }

    /**
     * Observes the provided event with the specified ID.
     *
     * @param E The type of the observed event.
     *
     * @param id The ID of the observed event.
     * @param type The class of the type of the observed event.
     * @param onEmit Called when the event is emitted.
     */
    internal fun <E : EventMessage> observeEvent(
        id: Message,
        type: KClass<E>,
        onEmit: (event: E) -> Unit
    ): Subscription =
        observeEvent(
            type,
            eq(EventMessageField(Field.named("id")), id),
            onEmit
        )

    /**
     * Observes the specified event that meets the `filter` condition.
     *
     * @param E The type of the observed event.
     *
     * @param type The class of the type of the observed event.
     * @param filter The selection condition for observed events.
     * @param onEmit Called when the event is emitted.
     */
    internal fun <E : EventMessage> observeEvent(
        type: KClass<E>,
        filter: EventFilter,
        onEmit: (event: E) -> Unit
    ): Subscription =
        clientRequest()
            .subscribeToEvent(type.java)
            .where(filter)
            .observe(onEmit)
            .post()

    /**
     * Subscribes to the event of the provided type and cancels itself after
     * the observer has worked.
     *
     * @param E The type of the observed event.
     *
     * @param id The ID of the observed event.
     * @param type The class of the type of the observed event.
     * @param onEmit Called when the event is emitted.
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
     * @param E The type of the observed entity.
     *
     * @param id The ID of the observed entity.
     * @param type The class of the type of the observed entity.
     * @param onUpdated Called when the entity is updated.
     */
    internal fun <E : EntityState> observeEntity(
        id: Message,
        type: KClass<E>,
        onUpdated: (entity: E) -> Unit
    ): Subscription =
        clientRequest()
            .subscribeTo(type.java)
            .byId(id)
            .observe(onUpdated)
            .post()

    /**
     * Stops observation of the provided subscription.
     */
    internal fun stopObservation(subscription: Subscription) {
        client.subscriptions()
            .cancel(subscription)
    }

    /**
     * Creates a new instance of the `ClientRequest` on behalf of [user][userId] if it exists,
     * or as guest if it doesn't.
     */
    private fun clientRequest(): ClientRequest =
        if (userId == null) {
            client.asGuest()
        } else {
            client.onBehalfOf(userId)
        }

    /**
     * Cancels all subscriptions.
     */
    internal fun close() {
        client.subscriptions()
            .cancelAll()
    }
}

/**
 * Holds details about the observation of the event.
 *
 * @param E The type of the observed event.
 *
 * @property id The ID of the observed event.
 * @property type The class of the type of the observed event.
 * @property callback Called when the event is emitted.
 */
internal data class EventObserver<E : EventMessage> internal constructor(
    internal val id: Message,
    internal val type: KClass<E>,
    internal val callback: (event: E) -> Unit
)
