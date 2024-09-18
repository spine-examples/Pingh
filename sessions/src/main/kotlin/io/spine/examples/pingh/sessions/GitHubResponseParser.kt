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

import io.spine.examples.pingh.github.User
import io.spine.examples.pingh.github.fromFragment
import io.spine.examples.pingh.github.rest.AccessTokenFragment
import io.spine.examples.pingh.github.rest.AccessTokenResponse
import io.spine.examples.pingh.github.rest.OrganizationsResponse
import io.spine.examples.pingh.github.rest.UserFragment
import io.spine.examples.pingh.github.rest.VerificationCodesFragment
import io.spine.examples.pingh.github.rest.VerificationCodesResponse
import io.spine.json.Json
import kotlin.reflect.KClass

/**
 * Parses `VerificationCodesResponse` from the JSON.
 *
 * @param json The string containing JSON with the `VerificationCodesResponse`.
 */
public fun KClass<VerificationCodesResponse>.parseJson(json: String): VerificationCodesResponse =
    VerificationCodesResponse::class.fromFragment(
        Json.fromJson(json, VerificationCodesFragment::class.java)
    )

/**
 * Parses `AccessTokenResponse` from the JSON.
 *
 * @param json The string containing JSON with the `AccessTokenResponse`.
 */
public fun KClass<AccessTokenResponse>.parseJson(json: String): AccessTokenResponse =
    AccessTokenResponse::class.fromFragment(
        Json.fromJson(json, AccessTokenFragment::class.java)
    )

/**
 * Parses `User` from the JSON.
 *
 * @param json The string containing JSON with the `User`.
 */
public fun KClass<User>.parseJson(json: String): User =
    User::class.fromFragment(
        Json.fromJson(json, UserFragment::class.java)
    )

/**
 * Parses `OrganizationsResponse` from the JSON.
 *
 * @param json The string containing JSON with the `OrganizationsResponse`.
 */
public fun KClass<OrganizationsResponse>.parseJson(json: String): OrganizationsResponse =
    Json.fromJson(json, OrganizationsResponse::class.java)
