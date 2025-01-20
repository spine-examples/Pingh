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

package io.spine.examples.pingh.testing.sessions.given

import com.google.protobuf.Timestamp
import io.spine.examples.pingh.github.DeviceCode
import io.spine.examples.pingh.github.RefreshToken
import io.spine.examples.pingh.github.rest.AccessTokenResponse
import io.spine.examples.pingh.github.rest.VerificationCodesResponse
import io.spine.examples.pingh.sessions.CannotObtainAccessToken
import io.spine.examples.pingh.sessions.GitHubAuthentication
import java.lang.Thread.sleep
import kotlin.jvm.Throws
import kotlin.time.Duration.Companion.milliseconds

/**
 * Implementation of `GitHubAuthentication` that get responses
 * from JSON files in the resource folder.
 *
 * Uses exclusively for testing.
 */
public class PredefinedGitHubAuthenticationResponses : GitHubAuthentication {

    /**
     * Whether the user has entered their user code.
     */
    private var isUserCodeEntered = false

    /**
     * Whether to freeze the execution of the [refreshAccessToken()][refreshAccessToken] method.
     *
     * If `true`, the method will be executed indefinitely, if `false`,
     * it will terminate without problems. The value can be changed during execution.
     */
    private var refreshingFrozen = false

    /**
     * The time when the personal access token issued by GitHub expires.
     */
    public var whenReceivedAccessTokenExpires: Timestamp? = null
        private set

    /**
     * Returns `VerificationCodesResponse` retrieved from a JSON file in the resource folder.
     */
    public override fun requestVerificationCodes(): VerificationCodesResponse =
        loadVerificationCodes()

    /**
     * Returns the `AccessTokenResponse` retrieved from a JSON file in the resource folder
     * if the user has entered their user code. Otherwise, throws
     * a `CannotObtainAccessToken` exception.
     */
    @Throws(CannotObtainAccessToken::class)
    public override fun requestAccessToken(deviceCode: DeviceCode): AccessTokenResponse =
        if (isUserCodeEntered) {
            val tokens = loadAccessToken()
            whenReceivedAccessTokenExpires = tokens.whenExpires
            tokens
        } else {
            throw CannotObtainAccessToken("authorization_pending")
        }

    /**
     * Returns the `AccessTokenResponse` retrieved from a JSON file in the resource folder.
     */
    override fun refreshAccessToken(refreshToken: RefreshToken): AccessTokenResponse {
        val tokens = loadRefreshedAccessToken()
        whenReceivedAccessTokenExpires = tokens.whenExpires
        while (refreshingFrozen) {
            sleep(timeBetweenExecutionAttempts.inWholeMilliseconds)
        }
        return tokens
    }

    /**
     * Marks that the user has entered their user code.
     *
     * After calling this method, the login verification will be successful.
     */
    public fun enterUserCode() {
        isUserCodeEntered = true
    }

    /**
     * Marks the update process as frozen.
     *
     * The token update process will remain incomplete until the service is unfrozen
     * by invoking the [unfreezeRefreshing()][unfreezeRefreshing] method.
     */
    public fun freezeRefreshing() {
        refreshingFrozen = true
    }

    /**
     * Unfreezes the token update process, allowing it to be completed.
     */
    public fun unfreezeRefreshing() {
        refreshingFrozen = false
    }

    /**
     * Resets the instance to its initial state.
     *
     * Once the instance is reset, the authentication process is also reset.
     * This means the user code has not been entered, and no responses
     * have been received from GitHub.
     */
    public fun reset() {
        isUserCodeEntered = false
        whenReceivedAccessTokenExpires = null
        unfreezeRefreshing()
    }

    private companion object {
        /**
         * The time after which the process will try to run again if it is frozen.
         */
        private val timeBetweenExecutionAttempts = 100.milliseconds
    }
}
