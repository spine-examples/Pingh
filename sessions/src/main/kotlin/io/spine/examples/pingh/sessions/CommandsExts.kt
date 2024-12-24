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

import com.google.protobuf.Timestamp
import io.spine.examples.pingh.sessions.command.LogUserIn
import io.spine.examples.pingh.sessions.command.LogUserOut
import io.spine.examples.pingh.sessions.command.UpdateToken
import io.spine.examples.pingh.sessions.command.VerifyUserLoginToGitHub
import io.spine.examples.pingh.sessions.command.VerifySession
import kotlin.reflect.KClass

/**
 * Creates a new `LogUserIn` command with the specified ID of the session.
 */
public fun KClass<LogUserIn>.withSession(id: SessionId): LogUserIn =
    LogUserIn.newBuilder()
        .setId(id)
        .vBuild()

/**
 * Creates a new `VerifyUserLoginToGitHub` command with the specified ID of the session.
 */
public fun KClass<VerifyUserLoginToGitHub>.withSession(id: SessionId): VerifyUserLoginToGitHub =
    VerifyUserLoginToGitHub.newBuilder()
        .setId(id)
        .vBuild()

/**
 * Creates a new `UpdateToken` command with the specified ID of the session
 * and the time the token update is requested.
 */
public fun KClass<UpdateToken>.with(
    id: SessionId,
    whenRequested: Timestamp
): UpdateToken =
    UpdateToken.newBuilder()
        .setId(id)
        .setWhenRequested(whenRequested)
        .vBuild()

/**
 * Creates a new `LogUserOut` command with the specified ID of the session.
 */
public fun KClass<LogUserOut>.withSession(id: SessionId): LogUserOut =
    LogUserOut.newBuilder()
        .setId(id)
        .vBuild()

/**
 * Creates a new `VerifySession` command with the specified ID of the session.
 */
public fun KClass<VerifySession>.with(id: SessionId): VerifySession =
    VerifySession.newBuilder()
        .setId(id)
        .vBuild()
