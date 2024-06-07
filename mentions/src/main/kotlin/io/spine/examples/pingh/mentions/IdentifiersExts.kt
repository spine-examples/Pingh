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

package io.spine.examples.pingh.mentions

import io.spine.examples.pingh.github.NodeId
import io.spine.examples.pingh.github.Username
import kotlin.reflect.KClass

/**
 * Creates a new [MentionId] with the specified [NodeId] and [Username].
 */
public fun KClass<MentionId>.buildBy(nodeId: NodeId, whomMentioned: Username): MentionId =
    MentionId.newBuilder()
        .setWhere(nodeId)
        .setUser(whomMentioned)
        .vBuild()

/**
 * Creates a new [GitHubClientId] with the specified [Username].
 */
public fun KClass<GitHubClientId>.buildBy(username: Username): GitHubClientId =
    GitHubClientId.newBuilder()
        .setUsername(username)
        .vBuild()

/**
 * Creates a new `UserMentionsId` with the specified GitHub username.
 */
public fun KClass<UserMentionsId>.buildBy(username: Username): UserMentionsId =
    UserMentionsId.newBuilder()
        .setUsername(username)
        .vBuild()
