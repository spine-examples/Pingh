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
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.spine.examples.pingh.github.DeviceCode
import io.spine.examples.pingh.github.GitHubApp
import io.spine.examples.pingh.github.RefreshToken
import io.spine.examples.pingh.github.rest.AccessTokenResponse
import io.spine.examples.pingh.github.rest.ErrorResponse
import io.spine.examples.pingh.github.rest.VerificationCodesResponse
import io.spine.json.Json
import kotlin.jvm.Throws
import kotlinx.coroutines.runBlocking

/**
 * Using the GitHub REST API generates access tokens for the user.
 *
 * @property gitHubApp Secrets used to make requests on behalf of the GitHub App.
 * @param engine The engine used to create the HTTP client.
 */
public class RemoteGitHubAuthentication(
    private val gitHubApp: GitHubApp,
    engine: HttpClientEngine
): GitHubAuthentication {

    /**
     * HTTP client on behalf of which requests is made.
     */
    private val client = HttpClient(engine)

    /**
     * Requests the user and device codes from the GitHub REST API to authenticate the user.
     */
    public override fun requestVerificationCodes(): VerificationCodesResponse =
        runBlocking {
            val response = client
                .authenticationRequest("https://github.com/login/device/code")
                .with(gitHubApp)
                .post()
            VerificationCodesResponse::class.parseJson(response.body())
        }

    /**
     * Requests the GitHub REST API the user's access token.
     *
     * To receive the token, the user must enter the user code issued
     * along with the specified device code.
     *
     * If an error occurs, GitHub sends an error message with a `200 OK` status,
     * similar to a successful request. Therefore, the content of the response body
     * should also be checked to ensure the request was successful.
     *
     * @param deviceCode The code used to verify the device.
     * @throws CannotObtainAccessToken if GitHub returns an error response
     *   when trying to get an access token.
     */
    @Throws(CannotObtainAccessToken::class)
    public override fun requestAccessToken(deviceCode: DeviceCode): AccessTokenResponse =
        runBlocking {
            val response = client
                .authenticationRequest("https://github.com/login/oauth/access_token")
                .with(gitHubApp)
                .with(deviceCode)
                .post()
            val body = response.body<String>()
            checkError(response.status, body)
            AccessTokenResponse::class.parseJson(body)
        }

    /**
     * Throws an `CannotObtainAccessToken` exception if the response status is not `200 OK` or
     * if the response body contains an error message.
     */
    @Throws(CannotObtainAccessToken::class)
    private fun checkError(status: HttpStatusCode, body: String) {
        if (status != HttpStatusCode.OK) {
            throw CannotObtainAccessToken("Something went wrong.")
        }
        val possibleErrorMessage = Json.fromJson(body, ErrorResponse::class.java)
        if (possibleErrorMessage.error.isNotEmpty()) {
            throw CannotObtainAccessToken(possibleErrorMessage.error)
        }
    }

    /**
     * Requests the GitHub REST API to generate a new user access token using the `RefreshToken`.
     */
    override fun refreshAccessToken(refreshToken: RefreshToken): AccessTokenResponse =
        runBlocking {
            val response = client
                .authenticationRequest("https://github.com/login/oauth/access_token")
                .with(gitHubApp)
                .with(refreshToken)
                .post()
            AccessTokenResponse::class.parseJson(response.body())
        }
}

/**
 * Creates authentication request builder and sets the request URL.
 */
private fun HttpClient.authenticationRequest(url: String): AuthenticationRequestBuilder =
    AuthenticationRequestBuilder(this, url)

/**
 * Builder for creating and sending authentication request on GitHub.
 */
private class AuthenticationRequestBuilder(
    private val client: HttpClient,
    private val url: String
) {

    /**
     * Secrets used to make requests on behalf of the GitHub App.
     */
    private var gitHubApp: GitHubApp? = null

    /**
     * The verification code that is used to verify the device.
     */
    private var deviceCode: DeviceCode? = null

    /**
     * The method name by which the app obtains the access token.
     */
    private var grantType: GrantType? = null

    /**
     * The token to renew the `PersonalAccessToken` when it expires.
     */
    private var refreshToken: RefreshToken? = null

    /**
     * Sets secrets used to make requests on behalf of the GitHub App.
     */
    fun with(gitHubApp: GitHubApp): AuthenticationRequestBuilder {
        this.gitHubApp = gitHubApp
        return this
    }

    /**
     * Sets the verification code that is used to verify the device.
     */
    fun with(deviceCode: DeviceCode): AuthenticationRequestBuilder {
        this.deviceCode = deviceCode
        grantType = GrantType.DEVICE_CODE
        return this
    }

    /**
     * Sets the token to renew the `PersonalAccessToken` when it expires.
     */
    fun with(refreshToken: RefreshToken): AuthenticationRequestBuilder {
        this.refreshToken = refreshToken
        grantType = GrantType.REFRESH_TOKEN
        return this
    }

    /**
     * Creates and sends request with specified data.
     *
     * @throws IllegalArgumentException if the GitHub App request data is not specified.
     */
    suspend fun post(): HttpResponse {
        checkNotNull(gitHubApp) { "GitHub App must be set." }
        return client.post(url) {
            url.configureParameters()
            headers.append("Accept", "application/vnd.github+json")
        }
    }

    /**
     * Configures request parameters.
     */
    private fun URLBuilder.configureParameters() {
        parameters.append("client_id", gitHubApp!!.id.value)
        if (grantType != null) {
            parameters.append("grant_type", grantType!!.value)
            if (grantType == GrantType.REFRESH_TOKEN) {
                parameters.append("client_secret", gitHubApp!!.secret.value)
            }
        }
        if (deviceCode != null) {
            parameters.append("device_code", deviceCode!!.value)
        }
        if (refreshToken != null) {
            parameters.append("refresh_token", refreshToken!!.value)
        }
    }
}

/**
 * The names of the methods by which the application obtains the access token.
 *
 * @see <a href="https://oauth.net/2/grant-types/">Grant types</a>
 */
private enum class GrantType(val value: String) {
    DEVICE_CODE("urn:ietf:params:oauth:grant-type:device_code"),
    REFRESH_TOKEN("refresh_token")
}
