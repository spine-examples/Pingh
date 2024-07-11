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

import com.google.protobuf.Duration
import com.google.protobuf.Message
import io.grpc.ManagedChannelBuilder
import io.spine.base.CommandMessage
import io.spine.base.EntityState
import io.spine.base.EventMessage
import io.spine.base.EventMessageField
import io.spine.base.Field
import io.spine.base.Time.currentTime
import io.spine.client.Client
import io.spine.client.ClientRequest
import io.spine.client.ConnectionConstants.DEFAULT_CLIENT_SERVICE_PORT
import io.spine.client.EventFilter.eq
import io.spine.client.Subscription
import io.spine.core.UserId
import io.spine.examples.pingh.github.Username
import io.spine.examples.pingh.mentions.GitHubClientId
import io.spine.examples.pingh.mentions.MentionId
import io.spine.examples.pingh.mentions.MentionView
import io.spine.examples.pingh.mentions.UserMentions
import io.spine.examples.pingh.mentions.UserMentionsId
import io.spine.examples.pingh.mentions.buildBy
import io.spine.examples.pingh.mentions.command.MarkMentionAsRead
import io.spine.examples.pingh.mentions.command.SnoozeMention
import io.spine.examples.pingh.mentions.command.UpdateMentionsFromGitHub
import io.spine.examples.pingh.mentions.event.MentionRead
import io.spine.examples.pingh.mentions.event.MentionSnoozed
import io.spine.examples.pingh.mentions.event.MentionsUpdateFromGitHubCompleted
import io.spine.examples.pingh.mentions.event.RequestMentionsFromGitHubFailed
import io.spine.examples.pingh.sessions.SessionId
import io.spine.examples.pingh.sessions.buildBy
import io.spine.examples.pingh.sessions.command.LogUserIn
import io.spine.examples.pingh.sessions.command.LogUserOut
import io.spine.examples.pingh.sessions.event.UserLoggedIn
import io.spine.examples.pingh.sessions.event.UserLoggedOut
import io.spine.protobuf.Durations2
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Interacts with [Pingh server][io.spine.examples.pingh.server] via gRPC.
 *
 * By default, client will open channel to 'localhost:[50051]
 * [io.spine.client.ConnectionConstants.DEFAULT_CLIENT_SERVICE_PORT]'.
 */
@Suppress("TooManyFunctions") // The client must contain multiple methods
// to interact with the server, which does not enable Detekt.
public class DesktopClient(
    address: String = "localhost",
    port: Int = DEFAULT_CLIENT_SERVICE_PORT
) {

    private companion object {
        /**
         * The default snooze time of mention.
         */
        private val defaultSnoozeTime = Durations2.hours(2)
    }

    private val client: Client
    private val user: UserId
    private var session: SessionId? = null

    init {
        val channel = ManagedChannelBuilder
            .forAddress(address, port)
            .usePlaintext()
            .build()
        client = Client
            .usingChannel(channel)
            .build()
        user = UserId.newBuilder()
            .setValue(UUID.randomUUID().toString())
            .vBuild()
    }

    /**
     * Logs the user in and remembers the session ID.
     */
    public fun logIn(
        username: Username,
        onSuccess: (event: UserLoggedIn) -> Unit = {}
    ) {
        val command = LogUserIn::class.buildBy(
            SessionId::class.buildBy(username)
        )
        observeEventOnce(command.id, UserLoggedIn::class) { event ->
            this.session = command.id
            onSuccess(event)
        }
        send(command)
    }

    /**
     * Logs the user out, cancels all subscriptions and clears the session ID.
     */
    public fun logOut(
        onSuccess: (event: UserLoggedOut) -> Unit = {}
    ) {
        checkNotNull(session) { "The user has not been logged in." }
        val command = LogUserOut::class.buildBy(session!!)
        observeEventOnce(command.id, UserLoggedOut::class) { event ->
            this.session = null
            onSuccess(event)
        }
        send(command)
    }

    /**
     * Updates the user's mentions.
     */
    public fun updateMentions(
        onSuccess: (event: MentionsUpdateFromGitHubCompleted) -> Unit = {},
        onFail: (event: RequestMentionsFromGitHubFailed) -> Unit = {}
    ) {
        checkNotNull(session) { "The user has not been logged in." }
        val command = UpdateMentionsFromGitHub::class.buildBy(
            GitHubClientId::class.buildBy(session!!.username)
        )
        observeCommandOutcome(
            command.id,
            MentionsUpdateFromGitHubCompleted::class,
            onSuccess,
            RequestMentionsFromGitHubFailed::class,
            onFail
        )
        send(command)
    }

    /**
     * Finds mentions of the user by their ID.
     *
     * @return List of `MentionView`s sorted by descending time of creation.
     */
    public fun findUserMentions(): List<MentionView> {
        checkNotNull(session) { "The user has not been logged in." }
        val userMentions = clientRequest()
            .select(UserMentions::class.java)
            .byId(UserMentionsId::class.buildBy(session!!.username))
            .run()
        if (userMentions.size == 0) {
            return listOf()
        }
        return userMentions[0]
            .mentionList
            .sortedByDescending { mention -> mention.whenMentioned.seconds }
    }

    /**
     * Marks the mention as snoozed.
     *
     * If snooze time is not specified, the mention will snooze the [defaultSnoozeTime].
     */
    public fun markMentionAsSnoozed(
        id: MentionId,
        snoozeTime: Duration = defaultSnoozeTime,
        onSuccess: (event: MentionSnoozed) -> Unit = {}
    ) {
        val command = SnoozeMention::class.buildBy(id, currentTime().add(snoozeTime))
        observeEventOnce(command.id, MentionSnoozed::class, onSuccess)
        send(command)
    }

    /**
     * Marks that the mention is read.
     */
    public fun markMentionAsRead(id: MentionId, onSuccess: (event: MentionRead) -> Unit = {}) {
        val command = MarkMentionAsRead::class.buildBy(id)
        observeEventOnce(command.id, MentionRead::class, onSuccess)
        send(command)
    }

    /**
     * Sends a command to the server on behalf of the user.
     */
    private fun send(command: CommandMessage) {
        clientRequest()
            .command(command)
            .postAndForget()
    }

    /**
     * Provides `ClientRequest` on behalf of logged-in user if it exists,
     * or as guest if it doesn't.
     */
    private fun clientRequest(): ClientRequest {
        if (session == null) {
            return client.asGuest()
        }
        return client.onBehalfOf(user)
    }

    /**
     * Observes the outcome of the command.
     *
     * When a success or failure event is emitted, subscriptions are cancelled.
     */
    private fun <S : EventMessage, F : EventMessage> observeCommandOutcome(
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
    private fun <E : EventMessage> observeEventOnce(
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
     * Stops observation by provided subscription.
     */
    private fun stopObservation(subscription: Subscription) {
        client.subscriptions()
            .cancel(subscription)
    }

    /**
     * Closes the client by shutting down the gRPC connection.
     */
    public fun close() {
        client.close()
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
}
