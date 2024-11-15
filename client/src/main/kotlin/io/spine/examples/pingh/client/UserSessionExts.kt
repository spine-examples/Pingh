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

package io.spine.examples.pingh.client

import io.spine.examples.pingh.client.FileStorage.loadOrDefault
import io.spine.examples.pingh.github.Username
import io.spine.examples.pingh.github.of
import io.spine.examples.pingh.sessions.SessionId
import kotlin.reflect.KClass

/**
 * The username to which the current session belongs.
 */
internal val UserSession.username: Username
    get() = this.id?.username ?: guestUsername

private val guestUsername: Username
    get() = Username::class.of("unauthenticated-guest")

/**
 * Returns `true` if the session is authenticated, and `false` if it is a guest session.
 */
internal fun UserSession.isAuthenticated() = !this.hasId()

/**
 * Saves the user session data to a file in the user's data directory.
 */
internal fun UserSession.save() {
    FileStorage.save(FileLocation.Session, this)
}

/**
 * Returns the guest session.
 */
internal fun KClass<UserSession>.guest(): UserSession = UserSession.getDefaultInstance()

/**
 * Creates authenticated `UserSession` with specified session ID.
 *
 * @param id The ID of the session.
 */
internal fun KClass<UserSession>.authenticated(id: SessionId): UserSession =
    UserSession.newBuilder()
        .setId(id)
        .vBuild()

/**
 * Loads the user session data from a file in the user's data directory.
 *
 * Returns a guest session if the file is empty.
 */
internal fun KClass<UserSession>.loadOrDefault(): UserSession =
    loadOrDefault(FileLocation.Session, UserSession::parseFrom) { UserSession::class.guest() }
