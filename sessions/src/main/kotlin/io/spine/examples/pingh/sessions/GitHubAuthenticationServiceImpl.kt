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

package io.spine.examples.pingh.sessions

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import io.spine.examples.pingh.github.DeviceCode
import io.spine.examples.pingh.github.rest.AccessTokenResponse
import io.spine.examples.pingh.github.rest.AuthenticationCodesResponse
import io.spine.examples.pingh.github.rest.ErrorResponse
import io.spine.json.Json
import kotlin.jvm.Throws
import kotlinx.coroutines.runBlocking

/**
 * Using the GitHub API generates access tokens for the user.
 *
 * @param clientId the client ID for the Pingh GitHub App.
 */
public class GitHubAuthenticationServiceImpl(
    private val clientId: String,
    engine: HttpClientEngine
): GitHubAuthenticationService {

    /**
     * HTTP client on behalf of which requests is made.
     */
    private val client = HttpClient(engine)

    /**
     * Requests the GitHub API the user and device codes to authenticate the user.
     */
    public override fun requestAuthenticationCodes(): AuthenticationCodesResponse =
        runBlocking {
            val response = client.post("https://github.com/login/device/code") {
                url {
                    parameters.append("client_id", clientId)
                }
                headers.apply {
                    append("Accept", "application/vnd.github+json")
                }
            }
            parseAuthenticationCodesResponse(response.body())
        }

    /**
     * Requests the GitHub API the user's access token.
     *
     * To receive the token, the user must enter the user code issued
     * along with the specified device code.
     *
     * @throws CannotObtainAccessToken if GitHub returns an error response
     *                                 when trying to get an access token.
     */
    @Throws(CannotObtainAccessToken::class)
    public override fun requestAccessToken(deviceCode: DeviceCode): AccessTokenResponse =
        runBlocking {
            val response = client.post("https://github.com/login/oauth/access_token") {
                url {
                    parameters.apply {
                        append("client_id", clientId)
                        append("device_code", deviceCode.value)
                        append("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
                    }
                    headers.apply {
                        append("Accept", "application/vnd.github+json")
                    }
                }
            }
            val body = response.body<String>()
            checkError(response.status, body)
            parseAccessTokenResponse(body)
        }

    private fun checkError(status: HttpStatusCode, body: String) {
        if (status != HttpStatusCode.OK) {
            throw CannotObtainAccessToken("Something went wrong.")
        }
        val possibleErrorMessage = Json.fromJson(body, ErrorResponse::class.java)
        if (possibleErrorMessage.error.isNotEmpty()) {
            throw CannotObtainAccessToken(possibleErrorMessage.error)
        }
    }
}
