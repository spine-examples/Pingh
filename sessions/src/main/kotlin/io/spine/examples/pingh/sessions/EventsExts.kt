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

import com.google.protobuf.Duration
import io.spine.examples.pingh.github.PersonalAccessToken
import io.spine.examples.pingh.github.UserCode
import io.spine.examples.pingh.sessions.event.UserCodeReceived
import io.spine.examples.pingh.sessions.event.UserIsNotLoggedIntoGitHub
import io.spine.examples.pingh.sessions.event.UserLoggedIn
import io.spine.examples.pingh.sessions.event.UserLoggedOut
import io.spine.net.Url
import kotlin.reflect.KClass

/**
 * Creates a new `UserTokenReceived` event with the specified ID of the session, the `UserCode`,
 * the URL where users need to enter their `UserCode`, the duration after which `UserCode` expires,
 * and the minimum duration that must pass before user can make a new access token request.
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
 * Creates a new `UserLoggedIn` event with the specified ID of the session
 * and `PersonalAccessToken`.
 */
public fun KClass<UserLoggedIn>.buildBy(id: SessionId, token: PersonalAccessToken): UserLoggedIn =
    UserLoggedIn.newBuilder()
        .setId(id)
        .setToken(token)
        .vBuild()

/**
 * Creates a new `UserIsNotLoggedIntoGitHub` event with the specified ID of the session.
 */
public fun KClass<UserIsNotLoggedIntoGitHub>.withSession(id: SessionId): UserIsNotLoggedIntoGitHub =
    UserIsNotLoggedIntoGitHub.newBuilder()
        .setId(id)
        .vBuild()

/**
 * Creates a new `UserLoggedOut` event with the specified ID of the session.
 */
public fun KClass<UserLoggedOut>.buildBy(id: SessionId): UserLoggedOut =
    UserLoggedOut.newBuilder()
        .setId(id)
        .vBuild()
