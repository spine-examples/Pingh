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
import io.spine.examples.pingh.sessions.of
import io.spine.examples.pingh.sessions.withSession
import io.spine.examples.pingh.sessions.command.LogUserIn
import io.spine.examples.pingh.sessions.command.VerifyUserLoginToGitHub
import io.spine.examples.pingh.sessions.event.UserCodeReceived
import io.spine.examples.pingh.sessions.event.UserIsNotLoggedIntoGitHub
import io.spine.examples.pingh.sessions.event.UserLoggedIn
import io.spine.examples.pingh.sessions.rejection.Rejections.UserIsNotMemberOfAnyPermittedOrganizations
import io.spine.examples.pingh.sessions.rejection.Rejections.UserLoggedInUsingDifferentAccount
import io.spine.net.Url
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Describes the login to GitHub via GitHub's device flow.
 *
 * There are several stages in this process:
 *
 * 1. [EnterUsername]: The user inputs their username to receive a user code.
 * 2. [VerifyLogin]: The user enters the user code on GitHub and confirms
 * the login within the Pingh app.
 * 3. [LoginFailed]: The login process failed due to an error that occurred during authentication.
 *
 * The flow is considered completed whenever the login is successfully
 * [confirmed][VerifyLogin.confirm] in the Pingh app.
 *
 * @property client Enables interaction with the Pingh server.
 * @property session The information about the current user session.
 */
public class LoginFlow internal constructor(
    private val client: DesktopClient,
    private val session: MutableStateFlow<UserSession?>
) {
    /**
     * Possible transitions between stages.
     *
     * Movement from one stage to another is restricted to specific stages,
     * and some stages may be final with no further transitions.
     * Each stage [change][moveToNextStage] verifies if the transition is permissible.
     */
    private val possibleTransitions = mapOf(
        EnterUsername::class to listOf(VerifyLogin::class),
        VerifyLogin::class to listOf(LoginFailed::class),
        LoginFailed::class to listOf(EnterUsername::class),
    )

    /**
     * Current stage of the GitHub login process.
     */
    private val stage: MutableStateFlow<LoginStage> = MutableStateFlow(EnterUsername())

    /**
     * Returns the immutable state of the current login stage.
     */
    public fun currentStage(): StateFlow<LoginStage> = stage

    /**
     * Switches the current stage to the passed one.
     *
     * @throws IllegalStateException if the transition of their current [stage]
     *   to the passed stage is not [allowed][possibleTransitions].
     */
    private fun moveToNextStage(stage: LoginStage) {
        val current = this.stage.value::class
        val possibleNext = possibleTransitions.getOrDefault(current, emptyList())
        val next = stage::class
        if (!possibleNext.contains(next)) {
            throw IllegalStateException(
                "Moving from $current stage to $next stage is not allowed; " +
                        "only $possibleNext stages is permitted."
            )
        }
        this.stage.value = stage
    }

    /**
     * Represents a stage in the GitHub login process.
     */
    public interface LoginStage

    /**
     * A stage of the login flow on which the user enters their GitHub username
     * and receives a user code in return.
     */
    public inner class EnterUsername internal constructor() : LoginStage {
        /**
         * Starts the GitHub login process and requests `UserCode`.
         *
         * @param username The username of the user logging in.
         * @param onSuccess Called when the user code is successfully received.
         */
        public fun requestUserCode(
            username: Username,
            onSuccess: (event: UserCodeReceived) -> Unit = {}
        ) {
            val command = LogUserIn::class.withSession(
                SessionId::class.of(username)
            )
            client.observeEvent(command.id, UserCodeReceived::class) { event ->
                session.value = UserSession(event.id)
                moveToNextStage(VerifyLogin(event))
                onSuccess(event)
            }
            client.send(command)
        }
    }

    /**
     * A stage of the login flow on which the user enters the received user code
     * into GitHub to verify their login.
     *
     * @param event The event received after the user enters their name.
     */
    @Suppress("MemberVisibilityCanBePrivate" /* Accessed from `desktop` module. */)
    public inner class VerifyLogin internal constructor(event: UserCodeReceived) : LoginStage {
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
         * Errors during verification cause the process to transition to the [LoginFailed] stage.
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
                EventObserver(command.id, UserLoggedInUsingDifferentAccount::class) { rejection ->
                    codeExpirationJob.cancel()
                    moveToNextStage(LoginFailed(rejection.cause))
                },
                EventObserver(
                    command.id,
                    UserIsNotMemberOfAnyPermittedOrganizations::class
                ) { rejection ->
                    codeExpirationJob.cancel()
                    moveToNextStage(LoginFailed(rejection.cause))
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
     * A stage in the login flow that indicates a failure in the process.
     *
     * Possible reasons for failure include:
     *
     * 1. The user is not a member of an authorized organization.
     * 2. The username obtained in [EnterUsername] step differs from the username
     * of the account used to complete [VerifyLogin] step.
     *
     * @param cause The reason for the login failure.
     */
    public inner class LoginFailed internal constructor(cause: String) : LoginStage {

        /**
         * The error message from the login process.
         */
        public val errorMessage: StateFlow<String> = MutableStateFlow(cause)

        /**
         * Starts the login process from the beginning.
         */
        public fun restartLogin() {
            moveToNextStage(EnterUsername())
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
 * An error message explaining the cause of `UserLoggedInUsingDifferentAccount` rejection.
 */
private val UserLoggedInUsingDifferentAccount.cause: String
    get() = "You entered \"${id.username.value}\" as the username but used the code " +
            "from \"${loggedInUsername.value}\" account. You must authenticate with " +
            "the account matching the username you initially provided."

/**
 * An error message explaining the cause of `UserIsNotMemberOfAnyPermittedOrganizations` rejection.
 */
private val UserIsNotMemberOfAnyPermittedOrganizations.cause: String
    get() = "You are not a member of an organization authorized to use the application. " +
            "You must belong to one of the following GitHub organizations: " +
            "${permittedOrganizationList.joinToString { it.login.value }}."
