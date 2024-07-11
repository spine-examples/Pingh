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

package io.spine.examples.pingh.client.e2e.given

import io.spine.examples.pingh.client.DesktopClient
import io.spine.examples.pingh.mentions.MentionId
import io.spine.examples.pingh.mentions.UserMentions
import io.spine.examples.pingh.mentions.UserMentionsId
import io.spine.examples.pingh.mentions.buildBy
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * Starts observing the update of the [UserMentions] projection.
 *
 * Subscribes to updates on the `UserMentions` entity.
 * A [CompletableFuture] is used to track these updates.
 *
 * It's crucial to begin observing the entity before sending a change command.
 * Otherwise, you might wait the update an entity that has already been modified.
 */
internal fun DesktopClient.observeUserMentions(id: MentionId): UserMentionsObserver {
    val future = CompletableFuture<Void>()
    val entityId = UserMentionsId::class.buildBy(id.user)
    this.observeEntityOnce(entityId, UserMentions::class) {
        future.complete(null)
    }
    return UserMentionsObserver(future)
}

/**
 * The observer for the [UserMentions] projection.
 *
 * Contains a `CompletableFuture` that will be marked as `completed` when
 * the projection is updated. This class is suitable for observing a single update because
 * after the first update, the `CompletableFuture` will be marked as `completed`.
 *
 * @see [DesktopClient.observeUserMentions]
 */
internal class UserMentionsObserver(private val future: CompletableFuture<Void>) {

    /**
     * Waits for the projection update to occur.
     *
     * If there is no update 2 seconds after the method call, a [TimeoutException] exception
     * will be thrown.
     *
     * @see [CompletableFuture.get]
     */
    internal fun waitUntilUpdate() {
        future.get(2, TimeUnit.SECONDS)
    }
}
