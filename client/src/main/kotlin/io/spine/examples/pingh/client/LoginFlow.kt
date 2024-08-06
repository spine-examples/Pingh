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
import io.spine.examples.pingh.sessions.buildBy
import io.spine.examples.pingh.sessions.command.LogUserIn
import io.spine.examples.pingh.sessions.command.VerifyUserLoginToGitHub
import io.spine.examples.pingh.sessions.event.UserCodeReceived
import io.spine.examples.pingh.sessions.event.UserIsNotLoggedIntoGitHub
import io.spine.examples.pingh.sessions.event.UserLoggedIn
import io.spine.net.Url
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * The control flow for the user login process.
 *
 * Enables sending commands to the Pingh server and stores the states of the login process.
 *
 * @param client enables interaction with the Pingh server.
 * @param session the information about the current user session.
 */
public class LoginFlow internal constructor(
    private val client: DesktopClient,
    private val session: MutableStateFlow<UserSession?>
) {
    /**
     * The current state of the login process.
     */
    public val state: MutableStateFlow<LoginState> = MutableStateFlow(LoginState.USERNAME_ENTERING)

    /**
     * Stores the event received after the user enters their name.
     *
     * This is required to initialize the verification state flow.
     */
    private var userCodeReceived: UserCodeReceived? = null

    /**
     * Initiates the username entering state of the login process.
     *
     * @throws IllegalStateException if the state of the login flow is not `USERNAME_ENTERING`.
     */
    public fun startUsernameEnteringFlow(): UsernameEnteringFlow {
        check(state.value == LoginState.USERNAME_ENTERING) {
            "State of the login must be `USERNAME_ENTERING`."
        }
        return UsernameEnteringFlow(client, session) { event ->
            state.value = LoginState.VERIFICATION
            userCodeReceived = event
        }
    }

    /**
     * Initiates the verification state of the login process.
     *
     * @throws IllegalStateException if the state of the login flow is not `VERIFICATION`.
     */
    public fun startVerificationFlow(): VerificationFlow {
        check(state.value == LoginState.VERIFICATION) {
            "State of the login must be `VERIFICATION`."
        }
        return VerificationFlow(client, session, userCodeReceived!!)
    }
}

/**
 * The control flow of the username entering state of the login process.
 *
 * @param client enables interaction with the Pingh server.
 * @param session the information about the current user session.
 * @param toVerificationState called when user codes are successfully received.
 */
public class UsernameEnteringFlow internal constructor(
    private val client: DesktopClient,
    private val session: MutableStateFlow<UserSession?>,
    private val toVerificationState: (UserCodeReceived) -> Unit
) {
    /**
     * Starts the login process and requests `UserCode`.
     */
    public fun requestUserCode(
        username: Username,
        onSuccess: (event: UserCodeReceived) -> Unit = {}
    ) {
        client.requestUserCode(username) { event ->
            session.value = UserSession(event.id)
            toVerificationState(event)
            onSuccess(event)
        }
    }
}

/**
 * The control flow of the verification state of the login process.
 *
 * @param client enables interaction with the Pingh server.
 * @param session the information about the current user session.
 * @param event event received in username entering state.
 */
@Suppress("MemberVisibilityCanBePrivate") // Some public properties are part
// of the public API for the desktop standalone project.
public class VerificationFlow internal constructor(
    private val client: DesktopClient,
    private val session: MutableStateFlow<UserSession?>,
    event: UserCodeReceived
) {
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
     *  The duration after which the [userCode] expires.
     */
    public val expiresIn: MutableStateFlow<Duration> = MutableStateFlow(event.expiresIn)

    /**
     * The minimum duration that must pass before user can make a new access token request.
     */
    public val interval: MutableStateFlow<Duration> = MutableStateFlow(event.interval)

    /**
     * Whether the user code is expired.
     */
    public val isUserCodeExpired: MutableStateFlow<Boolean> = MutableStateFlow(false)

    /**
     * Whether an access code request is available.
     */
    public val isAccessTokenRequestAvailable: MutableStateFlow<Boolean> =
        MutableStateFlow(true)

    /**
     * A job that marks a [userCode] as expired after the [time][expiresIn] has expired.
     */
    private lateinit var expirationObservationJob: Job

    init {
        startExpirationObservationJob()
    }

    /**
     * Starts a job that will mark the [userCode] as expired when [time][expiresIn] passes.
     */
    private fun startExpirationObservationJob() {
        isUserCodeExpired.value = false
        expirationObservationJob = makeJobWithDelay(expiresIn.value) {
            isUserCodeExpired.value = true
        }
    }

    /**
     * Checks whether the user has completed the login on GitHub and entered their user code.
     */
    public fun verify(
        onSuccess: (event: UserLoggedIn) -> Unit = {},
        onFail: (event: UserIsNotLoggedIntoGitHub) -> Unit = {}
    ) {
        val command = VerifyUserLoginToGitHub::class.buildBy(session.value!!.id)
        client.observeCommandOutcome(
            command.id,
            UserLoggedIn::class,
            { event ->
                expirationObservationJob.cancel()
                onSuccess(event)
            },
            UserIsNotLoggedIntoGitHub::class,
            { event ->
                blockTokenRequestsForInterval()
                onFail(event)
            }
        )
        client.send(command)
    }

    /**
     * Specifies that access token cannot be requested for within a certain [interval].
     */
    private fun blockTokenRequestsForInterval() {
        isAccessTokenRequestAvailable.value = false
        makeJobWithDelay(interval.value) {
            isAccessTokenRequestAvailable.value = true
        }
    }

    /**
     * Requests new `UserCode` and updates state of verification flow.
     */
    public fun requestNewUserCode(
        onSuccess: (event: UserCodeReceived) -> Unit = {}
    ) {
        client.requestUserCode(session.value!!.username) { event ->
            userCode.value = event.userCode
            verificationUrl.value = event.verificationUrl
            expiresIn.value = event.expiresIn
            interval.value = event.interval
            isAccessTokenRequestAvailable.value = true
            expirationObservationJob.cancel()
            startExpirationObservationJob()
            onSuccess(event)
        }
    }
}

/**
 * Starts the login process and requests `UserCode`.
 */
private fun DesktopClient.requestUserCode(
    username: Username,
    onSuccess: (event: UserCodeReceived) -> Unit = {}
) {
    val command = LogUserIn::class.buildBy(
        SessionId::class.buildBy(username)
    )
    observeEventOnce(command.id, UserCodeReceived::class, onSuccess)
    send(command)
}

/**
 * State of login process.
 */
public enum class LoginState {

    /**
     * Initial state where the user enters their `Username` and receives a `UserCode`.
     */
    USERNAME_ENTERING,

    /**
     * The final step where the user enters their `UserCode` into GitHub and
     * completes the login process in the Pingh app.
     */
    VERIFICATION
}

/**
 * Asynchronously performs work with a delay.
 */
private fun makeJobWithDelay(
    delayDuration: Duration,
    jobAction: () -> Unit
): Job =
    CoroutineScope(Dispatchers.Default).launch {
        delay(delayDuration.inWholeMilliseconds)
        jobAction()
    }
