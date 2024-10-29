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
 * @property session The information about the current user session.
 * @property moveToNextStage Switches the current stage to the [LoginFailed].
 * @param event The event received after the user enters their name.
 */
@Suppress("MemberVisibilityCanBePrivate" /* Accessed from `desktop` module. */)
public class VerifyLogin internal constructor(
    private val client: DesktopClient,
    private val session: MutableStateFlow<UserSession?>,
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
     * The minimum duration that must pass before user can make a new access token request.
     */
    public val interval: MutableStateFlow<Duration> = MutableStateFlow(event.interval)

    /**
     * Whether a new token can be asked from the external API.
     *
     * The contract of the external API assumes some delay that must pass
     * before a new token can be requested. Therefore, we should wait for this call
     * to become available.
     *
     * @see [interval]
     */
    public val canAskForNewTokens: MutableStateFlow<Boolean> = MutableStateFlow(true)

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
     * Job that marks a [userCode] as expired after the [time][expiresIn] has passed.
     */
    private lateinit var codeExpirationJob: Job

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
     * Checks whether the user has completed the login on GitHub and entered their user code.
     *
     * Rejections during verification cause the process to transition to the [LoginFailed] stage.
     *
     * @param onSuccess Called when the login is successfully verified.
     * @param onFail Called when login verification fails.
     */
    public fun confirm(
        onSuccess: (event: UserLoggedIn) -> Unit = {},
        onFail: (event: UserIsNotLoggedIntoGitHub) -> Unit = {}
    ) {
        val command = VerifyUserLoginToGitHub::class.withSession(session.value!!.id)
        client.observeEither(
            EventObserver(command.id, UserLoggedIn::class) { event ->
                codeExpirationJob.cancel()
                onSuccess(event)
            },
            EventObserver(command.id, UserIsNotLoggedIntoGitHub::class) { event ->
                preventAskingForNewTokens()
                onFail(event)
            },
            EventObserver(command.id, UsernameMismatch::class) { rejection ->
                codeExpirationJob.cancel()
                result = rejection.cause
                moveToNextStage()
            },
            EventObserver(command.id, NotMemberOfPermittedOrgs::class) { rejection ->
                codeExpirationJob.cancel()
                result = rejection.cause
                moveToNextStage()
            }
        )
        client.send(command)
    }

    /**
     * Prevents requests for new tokens during the [interval].
     */
    private fun preventAskingForNewTokens() {
        canAskForNewTokens.value = false
        invoke(interval.value) {
            canAskForNewTokens.value = true
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
        client.requestUserCode(session.value!!.username) { event ->
            userCode.value = event.userCode
            verificationUrl.value = event.verificationUrl
            expiresIn.value = event.expiresIn
            interval.value = event.interval
            canAskForNewTokens.value = true
            codeExpirationJob.cancel()
            watchForCodeExpiration()
            onSuccess(event)
        }
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
