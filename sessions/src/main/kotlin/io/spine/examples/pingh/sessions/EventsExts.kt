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

package io.spine.examples.pingh.sessions

import com.google.protobuf.Duration
import com.google.protobuf.Timestamp
import io.spine.examples.pingh.github.PersonalAccessToken
import io.spine.examples.pingh.github.UserCode
import io.spine.examples.pingh.sessions.event.SessionClosed
import io.spine.examples.pingh.sessions.event.TokenExpirationTimeUpdated
import io.spine.examples.pingh.sessions.event.TokenMonitoringFinished
import io.spine.examples.pingh.sessions.event.TokenMonitoringStarted
import io.spine.examples.pingh.sessions.event.TokenUpdated
import io.spine.examples.pingh.sessions.event.UserCodeReceived
import io.spine.examples.pingh.sessions.event.UserIsNotLoggedIntoGitHub
import io.spine.examples.pingh.sessions.event.UserLoggedIn
import io.spine.examples.pingh.sessions.event.UserLoggedOut
import io.spine.net.Url
import kotlin.reflect.KClass

/**
 * Creates a new `UserTokenReceived` event with the specified data.
 *
 * @param id The ID of the session.
 * @param userCode The verification code that displays so that the user can enter
 *   the code in a browser.
 * @param verificationUrl The GitHub page where users need to enter their `UserCode`.
 * @param expiresIn The duration after which `UserCode` expires.
 * @param interval The minimum duration that must pass before user can make
 *   a new access token request.
 */
public fun KClass<UserCodeReceived>.buildWith(
    id: SessionId,
    userCode: UserCode,
    verificationUrl: Url,
    expiresIn: Duration,
    interval: Duration
): UserCodeReceived =
    UserCodeReceived.newBuilder()
        .setId(id)
        .setUserCode(userCode)
        .setVerificationUrl(verificationUrl)
        .setExpiresIn(expiresIn)
        .setInterval(interval)
        .vBuild()

/**
 * Creates a new `UserLoggedIn` event with the specified ID of the session,
 * `PersonalAccessToken` and time this token expires.
 */
public fun KClass<UserLoggedIn>.with(
    id: SessionId,
    token: PersonalAccessToken,
    whenTokenExpires: Timestamp
): UserLoggedIn =
    UserLoggedIn.newBuilder()
        .setId(id)
        .setToken(token)
        .setWhenTokenExpires(whenTokenExpires)
        .vBuild()

/**
 * Creates a new `UserIsNotLoggedIntoGitHub` event with the specified ID of the session.
 */
public fun KClass<UserIsNotLoggedIntoGitHub>.withSession(id: SessionId): UserIsNotLoggedIntoGitHub =
    UserIsNotLoggedIntoGitHub.newBuilder()
        .setId(id)
        .vBuild()

/**
 * Creates a new `TokenUpdated` event with the specified ID of the session,
 * `PersonalAccessToken`, and the time the token expires.
 */
public fun KClass<TokenUpdated>.with(
    id: SessionId,
    token: PersonalAccessToken,
    whenTokenExpires: Timestamp
): TokenUpdated =
    TokenUpdated.newBuilder()
        .setId(id)
        .setToken(token)
        .setWhenTokenExpires(whenTokenExpires)
        .vBuild()

/**
 * Creates a new `UserLoggedOut` event with the specified ID of the session.
 */
public fun KClass<UserLoggedOut>.buildBy(id: SessionId): UserLoggedOut =
    UserLoggedOut.newBuilder()
        .setId(id)
        .vBuild()

/**
 * Creates a new `SessionClosed` event with the passed ID of the session.
 */
public fun KClass<SessionClosed>.with(id: SessionId): SessionClosed =
    SessionClosed.newBuilder()
        .setId(id)
        .vBuild()

/**
 * Creates a new `TokenMonitoringStarted` event
 * with the passed ID of the token monitoring process.
 */
public fun KClass<TokenMonitoringStarted>.with(id: TokenMonitorId): TokenMonitoringStarted =
    TokenMonitoringStarted.newBuilder()
        .setId(id)
        .vBuild()

/**
 * Creates a new `TokenMonitoringFinished` event
 * with the passed ID of the token monitoring process.
 */
public fun KClass<TokenMonitoringFinished>.with(id: TokenMonitorId): TokenMonitoringFinished =
    TokenMonitoringFinished.newBuilder()
        .setId(id)
        .vBuild()

/**
 * Creates a new `TokenExpirationTimeUpdated` event
 * with the passed ID of the token monitoring process.
 */
public fun KClass<TokenExpirationTimeUpdated>.with(id: TokenMonitorId): TokenExpirationTimeUpdated =
    TokenExpirationTimeUpdated.newBuilder()
        .setId(id)
        .vBuild()
