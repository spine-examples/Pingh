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

package io.spine.examples.pingh.client.session

import io.spine.examples.pingh.client.FileLocation.Companion.inAppDir
import io.spine.examples.pingh.client.FileStorage
import io.spine.examples.pingh.sessions.SessionId

/**
 * Manages application sessions, which can either be for a guest or an authenticated user.
 *
 * - A guest session provides access only to the login functionality and no other data.
 * - An authenticated session grants access to user mentions.
 *
 * All session changes are saved to a file in the user's data directory,
 * ensuring persistence across application restarts.
 */
internal class SessionManager {
    /**
     * A repository for storing current session data.
     */
    private val storage = FileStorage<SessionId>(inAppDir(".session"))

    /**
     * The current session with the Pingh server.
     */
    internal var current: SessionId
        private set

    init {
        current = storage.loadOrDefault(SessionId::parseFrom, guest)
    }

    /**
     * Sets a new session for the authenticated user.
     */
    internal fun establish(session: SessionId) {
        current = session
        storage.save(session)
    }

    /**
     * Replaces the current session with a guest session.
     */
    internal fun resetToGuest() {
        current = guest
        storage.clear()
    }

    /**
     * Returns `true` if the current [session][current] is a guest session.
     */
    internal fun isGuest(): Boolean = current == guest

    private companion object {
        private val guest = SessionId.getDefaultInstance()
    }
}
