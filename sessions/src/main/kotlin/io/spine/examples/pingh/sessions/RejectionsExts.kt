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

import io.spine.examples.pingh.github.Username
import io.spine.examples.pingh.sessions.rejection.NotMemberOfPermittedOrgs
import io.spine.examples.pingh.sessions.rejection.UsernameMismatch
import kotlin.reflect.KClass

/**
 * Creates a new `UsernameMismatch` rejection with the passed ID of the session,
 * name entered at the start of the login process, and name of the user whose account
 * was used for authentication.
 */
internal fun KClass<UsernameMismatch>.with(
    id: SessionId,
    expectedUser: Username,
    loggedInUser: Username
): UsernameMismatch =
    UsernameMismatch.newBuilder()
        .setId(id)
        .setExpectedUser(expectedUser)
        .setLoggedInUser(loggedInUser)
        .build()

/**
 * Creates a new `NotMemberOfPermittedOrgs` rejection with the passed ID of the session.
 */
internal fun KClass<NotMemberOfPermittedOrgs>.with(id: SessionId): NotMemberOfPermittedOrgs =
    NotMemberOfPermittedOrgs.newBuilder()
        .setId(id)
        .build()
