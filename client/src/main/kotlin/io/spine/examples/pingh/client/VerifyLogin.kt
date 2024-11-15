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
import io.spine.examples.pingh.client.ExponentialBackoffStrategy.ActionOutcome
import io.spine.examples.pingh.client.session.SessionManager
import io.spine.examples.pingh.github.UserCode
import io.spine.examples.pingh.github.Username
import io.spine.examples.pingh.sessions.SessionId
import io.spine.examples.pingh.sessions.command.LogUserIn
import io.spine.examples.pingh.sessions.command.VerifyUserLoginToGitHub
import io.spine.examples.pingh.sessions.event.UserCodeReceived
import io.spine.examples.pingh.sessions.event.UserIsNotLoggedIntoGitHub
import io.spine.examples.pingh.sessions.event.UserLoggedIn
import io.spine.examples.pingh.sessions.of
import io.spine.examples.pingh.sessions.rejection.Rejections.NotMemberOfPermittedOrgs
import io.spine.examples.pingh.sessions.rejection.Rejections.UsernameMismatch
import io.spine.examples.pingh.sessions.withSession
import io.spine.net.Url
import io.spine.protobuf.Durations2.minutes
import io.spine.protobuf.Durations2.seconds
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * A stage of the login flow on which the user enters the received user code
 * into GitHub to verify their login.
 *
 * @property client Enables interaction with the Pingh server.
 * @property session Manages application sessions.
 * @property moveToNextStage Switches the current stage to the [LoginFailed].
 * @param event The event received after the user enters their name.
 */
@Suppress("MemberVisibilityCanBePrivate" /* Accessed from `desktop` module. */)
public class VerifyLogin internal constructor(
    private val client: DesktopClient,
    private val session: SessionManager,
    private val moveToNextStage: () -> Unit,
    event: UserCodeReceived
) : LoginStage<String>() {

    /**
     * The code a user needs to enter on GitHub to confirm login to the app.
     */
    public val userCode: MutableStateFlow<UserCode> = MutableStateFlow(event.userCode)

    /**
     * The URL of the GitHub resource, where users will be entering the verification code
     * in scope of the device login flow.
     */
    public val verificationUrl: MutableStateFlow<Url> = MutableStateFlow(event.verificationUrl)

    /**
     * The duration after which the [userCode] expires.
     */
    public val expiresIn: MutableStateFlow<Duration> = MutableStateFlow(event.expiresIn)

    /**
     * Whether the user code is expired.
     *
     * @see [expiresIn]
     */
    public val isUserCodeExpired: MutableStateFlow<Boolean> = MutableStateFlow(false)

    /**
     * The minimum duration that must pass before user can make a new access token request.
     */
    private val interval = event.interval

    /**
     * Job that marks a [userCode] as expired after the [time][expiresIn] has passed.
     */
    private lateinit var codeExpirationJob: Job

    /**
     * A strategy that monitors GitHub login completion.
     *
     * The intervals between unsuccessful attempts increase exponentially,
     * and the step is considered successful upon a successful login.
     */
    private var retryStrategy: ExponentialBackoffStrategy? = null

    init {
        watchForCodeExpiration()
    }

    /**
     * Starts a job that will mark the [userCode] as expired when [time][expiresIn] passes.
     */
    private fun watchForCodeExpiration() {
        isUserCodeExpired.value = false
        codeExpirationJob = invoke(expiresIn.value) {
            isUserCodeExpired.value = true
        }
    }

    /**
     * Sends requests to verify authentication completion.
     *
     * If authentication is not complete, the request is retried
     * with exponentially increasing intervals.
     * The process automatically ends when the user code [expires][isUserCodeExpired].
     *
     * @param onSuccess Called when login is verified.
     */
    public fun waitForAuthCompletion(onSuccess: () -> Unit) {
        retryStrategy = ExponentialBackoffStrategy.builder()
            .perform { confirm() }
            .withMinDelay(interval)
            .withMaxDelay(backoffMaxDelay)
            .withFactor(exponentialBackoffFactor)
            .withTimeLimit(expiresIn.value)
            .doOnSuccess(onSuccess)
            .build()
        retryStrategy!!.start()
    }

    /**
     * Checks if the user has completed the login process on GitHub and entered their user code.
     *
     * Returns [Success status][ActionOutcome.Success] if authentication completes successfully.
     *
     * Returns [Rejection status][ActionOutcome.Rejection] if authentication completes with a
     * rejection, triggering a transition to the [LoginFailed] stage.
     *
     * Returns [Failure status][ActionOutcome.Failure] if:
     * - The user has not entered the code.
     * - The server did not respond within the [specified time][responseTimeout].
     */
    private fun confirm(): ActionOutcome {
        val future = CompletableFuture<ActionOutcome>()
        val command = VerifyUserLoginToGitHub::class.withSession(session.current)
        client.observeEither(
            EventObserver(command.id, UserLoggedIn::class) {
                codeExpirationJob.cancel()
                future.complete(ActionOutcome.Success)
            },
            EventObserver(command.id, UserIsNotLoggedIntoGitHub::class) {
                codeExpirationJob.cancel()
                future.complete(ActionOutcome.Failure)
            },
            EventObserver(command.id, UsernameMismatch::class) { rejection ->
                codeExpirationJob.cancel()
                future.complete(ActionOutcome.Rejection)
                result = rejection.cause
                moveToNextStage()
            },
            EventObserver(command.id, NotMemberOfPermittedOrgs::class) { rejection ->
                codeExpirationJob.cancel()
                future.complete(ActionOutcome.Rejection)
                result = rejection.cause
                moveToNextStage()
            }
        )
        client.send(command)
        return try {
            future.get(responseTimeout.seconds, TimeUnit.SECONDS)
        } catch (e: TimeoutException) {
            ActionOutcome.Failure
        }
    }

    /**
     * Requests a new `UserCode` on behalf of the current user.
     *
     * Resets the current stage to its initial state by canceling all active tasks
     * and updating fields values with data from the `UserCodeReceived` event.
     *
     * @param onSuccess Called when the user code is successfully received.
     */
    public fun requestNewUserCode(
        onSuccess: (event: UserCodeReceived) -> Unit = {}
    ) {
        client.requestUserCode(session.current.username) { event ->
            userCode.value = event.userCode
            verificationUrl.value = event.verificationUrl
            expiresIn.value = event.expiresIn
            codeExpirationJob.cancel()
            watchForCodeExpiration()
            onSuccess(event)
        }
    }

    /**
     * Cancels all processes initiated by this flow.
     */
    internal fun close() {
        codeExpirationJob.cancel()
        retryStrategy?.stop()
    }

    private companion object {
        /**
         * The exponential delay increase coefficient.
         */
        private const val exponentialBackoffFactor = 1.2

        /**
         * The maximum duration for the repeat interval in the exponential backoff strategy.
         */
        private val backoffMaxDelay = minutes(1)

        /**
         * The maximum time to wait for a server response.
         */
        private val responseTimeout = seconds(5)
    }
}

/**
 * Starts the GitHub login process and requests `UserCode`.
 */
private fun DesktopClient.requestUserCode(
    username: Username,
    onSuccess: (event: UserCodeReceived) -> Unit = {}
) {
    val command = LogUserIn::class.withSession(
        SessionId::class.of(username)
    )
    observeEventOnce(command.id, UserCodeReceived::class, onSuccess)
    send(command)
}

/**
 * Asynchronously performs work with a delay.
 */
private fun invoke(delay: Duration, action: () -> Unit): Job =
    CoroutineScope(Dispatchers.Default).launch {
        delay(delay.inWholeMilliseconds)
        action()
    }

/**
 * An error message explaining the cause of `UsernameMismatch` rejection.
 */
private val UsernameMismatch.cause: String
    get() = "You entered \"${expectedUser.value}\" as the username but used the code " +
            "issued for \"${loggedInUser.value}\" account. You must authenticate with " +
            "the account matching the username you initially provided."

/**
 * An error message explaining the cause of `NotMemberOfPermittedOrgs` rejection.
 */
@Suppress("UnusedReceiverParameter" /* Associated with the rejection but doesn't use its data. */)
private val NotMemberOfPermittedOrgs.cause: String
    get() = "You are not a member of an organization authorized to use the application."
