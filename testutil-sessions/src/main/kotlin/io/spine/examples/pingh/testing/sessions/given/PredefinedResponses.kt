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

import com.google.common.annotations.VisibleForTesting
import io.spine.examples.pingh.github.rest.AccessTokenResponse
import io.spine.examples.pingh.github.rest.OrganizationsResponse
import io.spine.examples.pingh.github.rest.TeamsResponse
import io.spine.examples.pingh.github.rest.VerificationCodesResponse
import io.spine.examples.pingh.sessions.parseJson

/**
 * Returns the response provided by [PredefinedGitHubAuthenticationResponses]
 * upon successful execution of the request for generate verification codes.
 */
@VisibleForTesting
public fun loadVerificationCodes(): VerificationCodesResponse {
    val jsonFile = PredefinedGitHubAuthenticationResponses::class.java
        .getResource("/github-responses/authentication-codes-response.json")
    checkNotNull(jsonFile)
    val json = jsonFile.readText()
    return VerificationCodesResponse::class.parseJson(json)
}

/**
 * Returns the response provided by [PredefinedGitHubAuthenticationResponses]
 * upon successful execution of the request for generate access token.
 */
@VisibleForTesting
public fun loadAccessToken(): AccessTokenResponse {
    val jsonFile = PredefinedGitHubAuthenticationResponses::class.java
        .getResource("/github-responses/access-token-response.json")
    checkNotNull(jsonFile)
    val json = jsonFile.readText()
    return AccessTokenResponse::class.parseJson(json)
}

/**
 * Returns the response provided by [PredefinedGitHubAuthenticationResponses]
 * upon successful execution of the request for refresh access token.
 */
@VisibleForTesting
public fun loadRefreshedAccessToken(): AccessTokenResponse {
    val jsonFile = PredefinedGitHubAuthenticationResponses::class.java
        .getResource("/github-responses/refreshed-access-token-response.json")
    checkNotNull(jsonFile)
    val json = jsonFile.readText()
    return AccessTokenResponse::class.parseJson(json)
}

/**
 * Returns the response provided by [PredefinedGitHubUsersResponses]
 * upon successful execution of the request for organizations in which the user is a member.
 */
@VisibleForTesting
public fun loadOrganizations(): OrganizationsResponse {
    val jsonFile = PredefinedGitHubUsersResponses::class.java
        .getResource("/github-responses/organizations-response.json")
    checkNotNull(jsonFile)
    val json = jsonFile.readText()
    return OrganizationsResponse::class.parseJson(" { item: $json } ")
}

/**
 * Returns the response provided by [PredefinedGitHubUsersResponses]
 * upon successful execution of the request for teams in which the user is a member.
 */
@VisibleForTesting
public fun loadTeams(): TeamsResponse {
    val jsonFile = PredefinedGitHubUsersResponses::class.java
        .getResource("/github-responses/teams-response.json")
    checkNotNull(jsonFile)
    val json = jsonFile.readText()
    return TeamsResponse::class.parseJson(" { item: $json } ")
}
