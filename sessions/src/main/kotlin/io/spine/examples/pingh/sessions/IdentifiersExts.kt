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
import com.google.protobuf.util.Timestamps
import io.spine.base.Time.currentTime
import io.spine.examples.pingh.github.Username
import kotlin.reflect.KClass

/**
 * Creates a new `SessionId` with the specified name of the user to which the session belongs
 * and the time when the session is created.
 */
public fun KClass<SessionId>.of(
    username: Username,
    whenCreated: Timestamp = currentTime()
): SessionId =
    SessionId.newBuilder()
        .setUsername(username)
        .setWhenCreated(whenCreated)
        .vBuild()

/**
 * Create a new `TokenMonitorId` with the ID of the session whose token is being monitored.
 */
public fun KClass<TokenMonitorId>.of(session: SessionId): TokenMonitorId =
    TokenMonitorId.newBuilder()
        .setSession(session)
        .vBuild()

/**
 * Converts a `SessionId` into a single-line string format for logging.
 */
internal fun SessionId.forLog(): String =
    "SessionId{username=${username.value}, whenCreated=${Timestamps.toString(whenCreated)}}"

/**
 * Converts a `TokenMonitorId` into a single-line string format for logging.
 */
internal fun TokenMonitorId.forLog(): String =
    "TokenMonitorId{session=${session.forLog()}}"
