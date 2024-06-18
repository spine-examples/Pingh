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
import io.spine.base.EventMessage
import io.spine.base.EventMessageField
import io.spine.base.Field
import io.spine.base.Time.currentTime
import io.spine.client.Client
import io.spine.client.ClientRequest
import io.spine.client.ConnectionConstants.DEFAULT_CLIENT_SERVICE_PORT
import io.spine.client.EventFilter
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
import io.spine.examples.pingh.mentions.event.MentionsUpdateFromGitHubCompleted
import io.spine.examples.pingh.mentions.event.RequestMentionsFromGitHubFailed
import io.spine.examples.pingh.sessions.SessionId
import io.spine.examples.pingh.sessions.command.LogUserIn
import io.spine.examples.pingh.sessions.command.LogUserOut
import io.spine.examples.pingh.sessions.event.UserLoggedIn
import io.spine.examples.pingh.sessions.event.UserLoggedOut
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Interacts with [Pingh server][io.spine.examples.pingh.server] via gRPC.
 *
 * By default, client will open channel to 'localhost:[50051]
 * [io.spine.client.ConnectionConstants.DEFAULT_CLIENT_SERVICE_PORT]'.
 */
public class DesktopClient(
    address: String = "localhost",
    port: Int = DEFAULT_CLIENT_SERVICE_PORT,
) {

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
        val command = LogUserIn.newBuilder()
            .setId(
                SessionId.newBuilder()
                    .setUsername(username)
                    .setWhenCreated(currentTime())
                    .vBuild()
            )
            .vBuild()
        observeEvent(command.id, UserLoggedIn::class) { event ->
            this.session = command.id
            onSuccess(event)
        }
        send(command)
    }

    /**
     * Logs the user out and clears the session ID.
     */
    public fun logOut(
        onSuccess: (event: UserLoggedOut) -> Unit = {}
    ) {
        checkNotNull(session) { "The user has not been logged in." }
        val command = LogUserOut.newBuilder()
            .setId(session)
            .vBuild()
        observeEvent(command.id, UserLoggedOut::class) { event ->
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
        val command = UpdateMentionsFromGitHub.newBuilder()
            .setId(GitHubClientId::class.buildBy(session!!.username))
            .setWhenRequested(currentTime())
            .vBuild()
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
     * Marks the mention as a 2-hour snooze.
     */
    public fun snoozeMention(id: MentionId) {
        val command = SnoozeMention.newBuilder()
            .setId(id)
            .setUntilWhen(currentTime().inTwoHours())
            .vBuild()
        send(command)
    }

    /**
     * Marks that the mention is read.
     */
    public fun readMention(id: MentionId) {
        val command = MarkMentionAsRead.newBuilder()
            .setId(id)
            .vBuild()
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
        var subscriptionOnSuccess: Subscription? = null
        var subscriptionOnFail: Subscription? = null
        subscriptionOnSuccess = observeEvent(id, successType) { event ->
            stopObservation(subscriptionOnSuccess!!)
            stopObservation(subscriptionOnFail!!)
            onSuccess(event)
        }
        subscriptionOnFail = observeEvent(id, failType) { event ->
            stopObservation(subscriptionOnSuccess)
            stopObservation(subscriptionOnFail!!)
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
            .where(EventFilter.eq(EventMessageField(Field.named("id")), id))
            .observe(onEmit)
            .post()

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
}
