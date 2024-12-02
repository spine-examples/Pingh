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

import io.spine.examples.pingh.sessions.SessionId
import io.spine.examples.pingh.sessions.event.UserCodeReceived
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Represents a stage in the GitHub login process.
 *
 * @param T The type of result produced upon executing this stage.
 *   If it is `Unit`, it means the stage produces no result.
 */
@Suppress("UnnecessaryAbstractClass" /* Avoids creating instances; only for inheritance. */)
public abstract class LoginStage<T> {
    /**
     * The result of executing this stage.
     *
     * Used to [move][LoginFlow.moveToNextStage] to the next stage,
     * must be specified before moving to the next stage.
     */
    internal var result: T? = null
}

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
 * The flow is considered [completed][LoginCompleted] whenever the login is successfully
 * [confirmed][VerifyLogin.confirm] in the Pingh app.
 *
 * @property client Enables interaction with the Pingh server.
 * @property establishSession Updates the application state when a session is established.
 */
public class LoginFlow internal constructor(
    private val client: DesktopClient,
    private val establishSession: (SessionId) -> Unit,
) {
    /**
     * Current stage of the GitHub login process.
     */
    private val stage: MutableStateFlow<LoginStage<*>> =
        MutableStateFlow(EnterUsername(client, ::moveToNextStage))

    /**
     * Returns the immutable state of the current login stage.
     */
    public fun currentStage(): StateFlow<LoginStage<*>> = stage

    /**
     * Return `true` if login process is completed.
     */
    public fun isCompleted(): Boolean = stage.value is LoginCompleted

    /**
     * Switches the [current stage][stage] to the next one.
     *
     * Some transitions rely on [result][LoginStage.result] from the previous stage
     * to create a new stage. Ensure previous stage's result is set.
     *
     * Transition order:
     *
     * - From [EnterUsername] stage to [VerifyLogin] stage;
     * - From `VerifyLogin` stage to [LoginFailed] stage;
     * - From `LoginFailed` stage back to `EnterUsername` stage.
     */
    private fun moveToNextStage() {
        when (stage.value) {
            is EnterUsername -> {
                checkNotNull(stage.value.result) {
                    "The `UserCodeReceived` event is not specified as the result " +
                            "of the `EnterUsername` stage."
                }
                stage.value = VerifyLogin(
                    client, establishSession, ::moveToNextStage,
                    stage.value.result as UserCodeReceived
                )
            }

            is VerifyLogin -> {
                if (stage.value.result == null) {
                    stage.value = LoginCompleted()
                } else {
                    stage.value = LoginFailed(::moveToNextStage, stage.value.result as String)
                }
            }

            is LoginFailed -> {
                stage.value = EnterUsername(client, ::moveToNextStage)
            }
        }
    }

    /**
     * Cancels any processes initiated during this stage of flow.
     */
    internal fun close() {
        when (val screenStage = stage.value) {
            is VerifyLogin -> screenStage.close()
        }
    }
}

/**
 * The final state of the login,
 * indicating that the login process has been successfully completed.
 */
private class LoginCompleted : LoginStage<Unit>()
