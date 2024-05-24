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

package io.spine.examples.pingh.github

import io.spine.net.Url
import kotlin.reflect.KClass

/**
 * Creates a new [NodeId] with the specified string value.
 */
public fun KClass<NodeId>.buildBy(value: String): NodeId =
    NodeId.newBuilder()
        .setValue(value)
        .vBuild()

/**
 * Creates a new [Username] with the specified string value.
 */
public fun KClass<Username>.buildBy(value: String): Username =
    Username.newBuilder()
        .setValue(value)
        .vBuild()

/**
 * Creates a new [PersonalAccessToken] with the specified string value.
 */
public fun KClass<PersonalAccessToken>.buildBy(value: String): PersonalAccessToken =
    PersonalAccessToken.newBuilder()
        .setValue(value)
        .vBuild()

/**
 * Creates a new [Url] with the specified string value.
 */
public fun KClass<Url>.buildBy(spec: String): Url =
    Url.newBuilder()
        .setSpec(spec)
        .vBuild()

/**
 * Creates a new [User] with the specified [Username] and avatar [Url].
 */
public fun KClass<User>.buildBy(username: Username, avatarUrl: Url): User =
    User.newBuilder()
        .setUsername(username)
        .setAvatarUrl(avatarUrl)
        .vBuild()

/**
 * Creates a new [User] with the specified username and avatar URL.
 */
public fun KClass<User>.buildBy(username: String, avatarUrl: String): User =
    this.buildBy(
        Username::class.buildBy(username),
        Url::class.buildBy(avatarUrl)
    )
