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

package io.spine.examples.pingh.client

import com.google.protobuf.util.Timestamps
import io.spine.examples.pingh.github.Username
import io.spine.examples.pingh.github.of
import io.spine.examples.pingh.sessions.SessionId
import io.spine.examples.pingh.sessions.of

/**
 * Information about the current user session.
 *
 * A session can be in one of two states:
 *
 * - Guest: The user does not have access to mentions or settings.
 * - Authenticated: The user has access to personal data and can receive mentions
 *
 * @param whoCreated The name of the creator of this session.
 * @param whenCreated The time when the session was created
 *   in [RFC 3339](https://datatracker.ietf.org/doc/html/rfc3339) format.
 */
internal data class UserSession(
    private var whoCreated: String? = null,
    private var whenCreated: String? = null
) {
    /**
     * The username to which the current session belongs.
     */
    internal val username: Username
        get() = Username::class.of(whoCreated ?: "guest")

    /**
     * The ID of current session.
     */
    internal val id: SessionId
        get() = SessionId::class.of(username, Timestamps.parse(whenCreated))

    /**
     * Authenticates the current session.
     *
     * @param id The ID of the session.
     */
    internal fun authenticate(id: SessionId) {
        whoCreated = id.username.value
        whenCreated = Timestamps.toString(id.whenCreated)
    }

    /**
     * Turns a session into a guest session.
     */
    internal fun guest() {
        whoCreated = null
        whenCreated = null
    }

    /**
     * Returns true if the session is authenticated, and false if it is a guest session.
     */
    internal fun isAuthenticated() = whoCreated != null && whenCreated != null

    /**
     * Saves the user session data to a file in the user's data directory.
     */
    internal fun save() {
        FileStorage.save(location, this)
    }

    internal companion object {
        /**
         * The location on disk of the file that stores the user session data.
         */
        private val location = FileLocation.Session

        /**
         * Loads the user session data from a file in the user's data directory.
         *
         * Returns a null session if the file is empty.
         */
        internal fun loadOrDefault(): UserSession =
            FileStorage.loadOrDefault(location) { UserSession() }
    }
}
