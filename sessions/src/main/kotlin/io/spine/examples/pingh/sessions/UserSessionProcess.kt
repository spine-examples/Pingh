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

import io.spine.examples.pingh.github.PersonalAccessToken
import io.spine.examples.pingh.github.buildBy
import io.spine.examples.pingh.sessions.command.LogUserIn
import io.spine.examples.pingh.sessions.command.LogUserOut
import io.spine.examples.pingh.sessions.event.UserLoggedIn
import io.spine.examples.pingh.sessions.event.UserLoggedOut
import io.spine.server.command.Assign
import io.spine.server.procman.ProcessManager

/**
 * Coordinates session management, that is, user login and logout.
 */
public class UserSessionProcess :
    ProcessManager<SessionId, UserSession, UserSession.Builder>() {

    /**
     * Emits event when a user logs in.
     */
    @Assign
    internal fun handle(command: LogUserIn): UserLoggedIn {
        initState(command)
        return UserLoggedIn::class.buildBy(
            command.id,
            PersonalAccessToken::class.buildBy("token")
        )
    }

    private fun initState(command: LogUserIn) {
        builder().setId(command.id)
    }

    /**
     * Emits event when a user logs out.
     */
    @Assign
    internal fun handle(command: LogUserOut): UserLoggedOut {
        deleted = true
        return UserLoggedOut::class.buildBy(command.id)
    }
}
