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

@file:Suppress("UnusedReceiverParameter" /* Class extensions don't use class as a parameter. */)

package io.spine.examples.pingh.sessions.given

import com.google.protobuf.Timestamp
import io.spine.examples.pingh.github.DeviceCode
import io.spine.examples.pingh.github.RefreshToken
import io.spine.examples.pingh.github.Username
import io.spine.examples.pingh.github.of
import io.spine.examples.pingh.sessions.SessionId
import io.spine.examples.pingh.sessions.UserSession
import io.spine.examples.pingh.sessions.buildBy
import io.spine.examples.pingh.sessions.of
import io.spine.examples.pingh.sessions.buildWith
import io.spine.examples.pingh.sessions.event.TokenRefreshed
import io.spine.examples.pingh.sessions.event.UserCodeReceived
import io.spine.examples.pingh.sessions.event.UserLoggedIn
import io.spine.examples.pingh.sessions.permittedOrganizations
import io.spine.examples.pingh.sessions.rejection.Rejections.UsernameMismatch
import io.spine.examples.pingh.sessions.rejection.Rejections.OrgAccessDenied
import io.spine.examples.pingh.sessions.with
import io.spine.examples.pingh.testing.sessions.given.loadAccessToken
import io.spine.examples.pingh.testing.sessions.given.loadRefreshedAccessToken
import io.spine.examples.pingh.testing.sessions.given.loadVerificationCodes
import io.spine.testing.TestValues.randomString
import kotlin.reflect.KClass

/**
 * Creates a new `SessionId` with a randomly generated `Username`
 * and creation time specified as now.
 */
internal fun KClass<SessionId>.generate(): SessionId = of(Username::class.of(randomString()))

/**
 * Creates a new `UserSession` with the specified session ID.
 *
 * Additionally, the device code and the refresh token can be specified.
 */
internal fun KClass<UserSession>.with(
    id: SessionId,
    deviceCode: DeviceCode? = null,
    refreshToken: RefreshToken? = null
): UserSession =
    with(UserSession.newBuilder()){
        this.id = id
        if (deviceCode != null) {
            this.deviceCode = deviceCode
        }
        if (refreshToken != null) {
            this.refreshToken = refreshToken
        }
        vBuild()
    }

/**
 * Creates a new `UserSession` with the specified session ID and device code
 * from the predefined GitHub response.
 */
internal fun expectedUserSessionWithDeviceCode(id: SessionId): UserSession =
    with(loadVerificationCodes()) {
        UserSession::class.with(id, deviceCode)
    }

/**
 * Creates a new `UserSession` with the specified session ID and refresh token
 * from the predefined GitHub response.
 */
internal fun expectedUserSessionWithRefreshToken(id: SessionId): UserSession =
    with(loadAccessToken()) {
        UserSession::class.with(id, refreshToken = refreshToken)
    }

/**
 * Creates a new `UserSession` with the passed session ID and refresh token
 * from the predefined GitHub response to the token update request.
 */
internal fun expectedUserSessionAfterTokenRefresh(id: SessionId): UserSession =
    with(loadRefreshedAccessToken()) {
        UserSession::class.with(id, refreshToken = refreshToken)
    }

/**
 * Creates a new `UserCodeReceived` event with the specified session ID and
 * data from the predefined GitHub response.
 */
internal fun expectedUserCodeReceivedEvent(id: SessionId): UserCodeReceived =
    with(loadVerificationCodes()) {
        UserCodeReceived::class.buildWith(id, userCode, verificationUrl, expiresIn, interval)
    }

/**
 * Creates a new `UserLoggedIn` event with the specified session ID and
 * data from the predefined GitHub response.
 */
internal fun expectedUserLoggedInEvent(id: SessionId): UserLoggedIn =
    with(loadAccessToken()) {
        UserLoggedIn::class.buildBy(id, accessToken)
    }

/**
 * Creates a new `TokenRefreshed` event with the passed session ID,
 * the time the access token was refreshed, and data from the predefined GitHub response.
 */
internal fun expectedTokenRefreshedEvent(id: SessionId, whenRefreshed: Timestamp): TokenRefreshed =
    with(loadRefreshedAccessToken()) {
        TokenRefreshed::class.with(id, accessToken, whenRefreshed)
    }

/**
 * Creates a new `UsernameMismatch` rejection with the passed ID of the session
 * and name of the user whose account was used for authentication.
 */
internal fun KClass<UsernameMismatch>.with(
    id: SessionId,
    loggedInUsername: Username
): UsernameMismatch =
    UsernameMismatch.newBuilder()
        .setId(id)
        .setLoggedInUsername(loggedInUsername)
        .vBuild()

/**
 * Creates a new `OrgAccessDenied` rejection
 * with the passed ID of the session.
 */
internal fun KClass<OrgAccessDenied>.with(id: SessionId): OrgAccessDenied =
    OrgAccessDenied.newBuilder()
        .setId(id)
        .addAllPermittedOrganization(permittedOrganizations)
        .vBuild()
