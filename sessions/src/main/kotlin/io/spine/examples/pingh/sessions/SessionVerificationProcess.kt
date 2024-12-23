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

import io.spine.examples.pingh.sessions.command.VerifySession
import io.spine.examples.pingh.sessions.event.ActiveSessionAdded
import io.spine.examples.pingh.sessions.event.InactiveSessionRemoved
import io.spine.examples.pingh.sessions.event.SessionExpired
import io.spine.examples.pingh.sessions.event.SessionVerificationFailed
import io.spine.examples.pingh.sessions.event.SessionVerified
import io.spine.examples.pingh.sessions.event.UserLoggedIn
import io.spine.examples.pingh.sessions.event.UserLoggedOut
import io.spine.server.command.Assign
import io.spine.server.event.React
import io.spine.server.procman.ProcessManager
import io.spine.server.tuple.EitherOf2

/**
 * The process of verifying whether a session is active.
 *
 * @see [SessionVerification]
 */
internal class SessionVerificationProcess :
    ProcessManager<SessionVerificationId, SessionVerification, SessionVerification.Builder>() {

    /**
     * Verifies whether the session is active.
     *
     * If the session is active, `SessionVerified` event is emitted;
     * if it is inactive, `SessionVerificationFailed` event is emitted.
     */
    @Assign
    internal fun handle(
        command: VerifySession
    ): EitherOf2<SessionVerified, SessionVerificationFailed> =
        if (state().activeSessionsList.contains(command.session)) {
            EitherOf2.withA(SessionVerified::class.with(command.id))
        } else {
            EitherOf2.withB(SessionVerificationFailed::class.with(command.id))
        }

    /**
     * Adds a new session to the list of active user sessions
     * when the user logged in.
     */
    @React
    internal fun on(event: UserLoggedIn): ActiveSessionAdded {
        builder().addActiveSessions(event.id)
        return ActiveSessionAdded::class.with(
            SessionVerificationId::class.of(event.id.username)
        )
    }

    /**
     * Remove a session from the list of active user sessions
     * when the user logged out.
     */
    @React
    internal fun on(event: UserLoggedOut): InactiveSessionRemoved = remove(event.id)

    /**
     * Remove a session from the list of active user sessions
     * when the session has expired.
     */
    @React
    internal fun on(event: SessionExpired): InactiveSessionRemoved = remove(event.id)

    private fun remove(session: SessionId): InactiveSessionRemoved {
        val id = builder().activeSessionsList.indexOfFirst { it.equals(session) }
        if (id != -1) {
            builder().removeActiveSessions(id)
        }
        return InactiveSessionRemoved::class.with(
            SessionVerificationId::class.of(session.username)
        )
    }
}
