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
import io.spine.examples.pingh.sessions.withSession
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * The control flow of the user login process.
 *
 * The flow consists of two consecutive stages. To successfully complete the login process,
 * first enter a username and obtain a user code, then verify the login.
 *
 * @param client enables interaction with the Pingh server.
 * @param session the information about the current user session.
 */
public class LoginFlow internal constructor(
    private val client: DesktopClient,
    private val session: MutableStateFlow<UserSession?>
) {
    /**
     * Current stage of the login process.
     */
    private val stage: MutableStateFlow<LoginStage> =
        MutableStateFlow(EnterUsername(client, session, this::moveToNextStage))

    /**
     * Returns the immutable state of the current login stage.
     */
    public fun currentStage(): StateFlow<LoginStage> = stage

    /**
     * Updates value of the current stage of the login process.
     */
    private fun moveToNextStage(nextStage: LoginStage) {
        stage.value = nextStage
    }
}

/**
 * Stages of login process.
 */
public interface LoginStage

/**
 * Manages the stage of the login process where the user must enter their username
 * will receive a user code in return.
 *
 * @param client enables interaction with the Pingh server.
 * @param session the information about the current user session.
 * @param moveToNextStage updates value of the current login stage.
 */
public class EnterUsername internal constructor(
    private val client: DesktopClient,
    private val session: MutableStateFlow<UserSession?>,
    private val moveToNextStage: (LoginStage) -> Unit
) : LoginStage {

    /**
     * Starts the login process and requests `UserCode`.
     *
     * @param username the username of the user logging in.
     * @param onSuccess called when the user code is successfully received.
     */
    public fun requestUserCode(
        username: Username,
        onSuccess: (event: UserCodeReceived) -> Unit = {}
    ) {
        client.requestUserCode(username) { event ->
            session.value = UserSession(event.id)
            moveToNextStage(VerifyLogin(client, session, event))
            onSuccess(event)
        }
    }
}

/**
 * Manages the stage of the login process where the user must enter the received user code
 * into GitHub to verify their login.
 *
 * @param client enables interaction with the Pingh server.
 * @param session provides information about the current user session.
 * @param event event received after the user enters their name.
 */
@Suppress("MemberVisibilityCanBePrivate" /* Accessed from `desktop` module. */)
public class VerifyLogin internal constructor(
    private val client: DesktopClient,
    private val session: MutableStateFlow<UserSession?>,
    event: UserCodeReceived
) : LoginStage {

    /**
     * The code a user needs to enter on GitHub to confirm login to the app.
     */
    public val userCode: MutableStateFlow<UserCode> =
        MutableStateFlow(event.userCode)

    /**
     * The URL of the GitHub resource, where users will be entering the verification code
     * in scope of the device login flow.
     */
    public val verificationUrl: MutableStateFlow<Url> =
        MutableStateFlow(event.verificationUrl)

    /**
     * The minimum duration that must pass before user can make a new access token request.
     */
    public val interval: MutableStateFlow<Duration> =
        MutableStateFlow(event.interval)

    /**
     * Whether we can ask for a new user token from the external API.
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
    public val expiresIn: MutableStateFlow<Duration> =
        MutableStateFlow(event.expiresIn)

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
     * @param onSuccess called when the login is successfully verified.
     * @param onFail called when login verification fails.
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
     * Requests new `UserCode` and updates state of the verification flow.
     *
     * @param onSuccess called when the user code is successfully received.
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
 * Starts the login process and requests `UserCode`.
 */
private fun DesktopClient.requestUserCode(
    username: Username,
    onSuccess: (event: UserCodeReceived) -> Unit = {}
) {
    val command = LogUserIn::class.withSession(
        SessionId::class.buildBy(username)
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
