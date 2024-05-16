/*
 * Copyright 2024, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.examples.pingh.sessions.given

import io.spine.base.Time.currentTime
import io.spine.examples.pingh.github.Username
import io.spine.examples.pingh.sessions.SessionId
import io.spine.examples.pingh.sessions.UserSession
import io.spine.examples.pingh.sessions.command.LogUserIn
import io.spine.examples.pingh.sessions.command.LogUserOut
import io.spine.testing.TestValues.randomString

/**
 * Returns the identifier for the session.
 *
 * The creator's [Username] is selected randomly,
 * and the creation time is set at the current moment.
 */
public fun sessionId(): SessionId =
    sessionIdBy(
        with(Username.newBuilder()) {
            value = randomString()
            vBuild()
        })

/**
 * Returns the identifier for the session based on the passed [Username].
 *
 * The creation time is indicated at the current moment.
 */
public fun sessionIdBy(name: Username): SessionId =
    with(SessionId.newBuilder()) {
        username = name
        whenCreated = currentTime()
        vBuild()
    }

/**
 * Returns the [UserSession] process manager, created using the passed [SessionId].
 */
public fun userSession(session: SessionId): UserSession =
    with(UserSession.newBuilder()) {
        id = session
        vBuild()
    }

/**
 * Returns the [LogUserIn] command, created using the passed [SessionId].
 */
public fun logUserIn(sessionId: SessionId): LogUserIn =
    with(LogUserIn.newBuilder()) {
        id = sessionId
        vBuild()
    }

/**
 * Returns the [LogUserOut] command, created using the passed [SessionId].
 */
public fun logUserOut(sessionId: SessionId): LogUserOut =
    with(LogUserOut.newBuilder()) {
        id = sessionId
        vBuild()
    }
