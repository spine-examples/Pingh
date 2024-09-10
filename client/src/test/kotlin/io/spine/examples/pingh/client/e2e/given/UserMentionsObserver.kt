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

import io.spine.examples.pingh.client.PinghApplication
import io.spine.examples.pingh.github.Username
import io.spine.examples.pingh.mentions.UserMentions
import io.spine.examples.pingh.mentions.UserMentionsId
import io.spine.examples.pingh.mentions.of
import java.lang.Thread.sleep
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Starts observing the update of the [UserMentions] projection.
 *
 * Subscribes to updates on the `UserMentions` entity.
 * A [CompletableFuture] is used to track these updates.
 *
 * The passed [number][expectedUpdatesCount] of entity updates must occur
 * for the observation to be considered successfully completed.
 *
 * It's crucial to begin observing the entity before sending a change command.
 * Otherwise, you might wait the update an entity that has already been modified.
 */
internal fun PinghApplication.observeUserMentions(
    user: Username,
    expectedUpdatesCount: Int = 1
): UserMentionsObserver {
    require(expectedUpdatesCount > 0) { "Expected count of updates must be positive." }
    val future = CompletableFuture<Void>()
    val entityId = UserMentionsId::class.of(user)
    var current = 0
    val subscription = client.observeEntity(entityId, UserMentions::class) {
        current++
        if (current == expectedUpdatesCount) {
            future.complete(null)
        }
    }
    return UserMentionsObserver(future) { client.cancel(subscription) }
}

/**
 * The observer for the [UserMentions] projection.
 *
 * Contains a `CompletableFuture` that will be marked as `completed` when
 * the projection is updated. This class is suitable for observing a single update because
 * after the first update, the `CompletableFuture` will be marked as `completed`.
 *
 * @see [PinghApplication.observeUserMentions]
 */
internal class UserMentionsObserver(
    private val future: CompletableFuture<Void>,
    private val cancelSubscription: () -> Unit
) {

    /**
     * Waits for the projection update to occur.
     *
     * If there is no update 2 seconds after the method call, a [TimeoutException] exception
     * will be thrown.
     *
     * @see [CompletableFuture.get]
     */
    internal fun waitUntilUpdate() {
        try {
            future.get(10, TimeUnit.SECONDS)
            sleep(100) // Ensures consistency with the storage.
        } finally {
            cancelSubscription()
        }
    }
}
