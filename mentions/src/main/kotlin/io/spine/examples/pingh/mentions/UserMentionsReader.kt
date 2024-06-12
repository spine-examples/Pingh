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

import io.spine.client.ActorRequestFactory
import io.spine.client.Query
import io.spine.client.QueryResponse
import io.spine.core.ActorContext
import io.spine.core.EventContext
import io.spine.examples.pingh.github.Username
import io.spine.grpc.MemoizingObserver
import io.spine.protobuf.AnyPacker.unpack
import io.spine.server.stand.Stand

/**
 * Reader for [UserMentionsProjection].
 */
public class UserMentionsReader(
    private val stand: Stand
) {

    /**
     * Returns all saved mentions for a specific user by name.
     */
    public fun mentionsOfUser(username: Username, context: EventContext): List<MentionView> {
        val id = UserMentionsId::class.buildBy(username)
        val userMentions = read(setOf(id), context.actorContext())
        if (userMentions.size != 1) {
            throw IllegalStateException("Must be one projection per identifier.")
        }
        return userMentions
            .flatMap { it.mentionList }
    }

    /**
     * Reads projections by identifiers on behalf of the actor from the context.
     */
    private fun read(ids: Set<UserMentionsId>, context: ActorContext): Set<UserMentions> {
        val queryFactory = ActorRequestFactory
            .fromContext(context)
            .query()
        val query = queryFactory.byIds(UserMentions::class.java, ids)
        return executeAndUnpack(query)
    }

    /**
     * Executes the query and converts the results to `UserMentions` type.
     */
    private fun executeAndUnpack(query: Query): Set<UserMentions> {
        val observer = MemoizingObserver<QueryResponse>()
        stand.execute(query, observer)
        return observer.firstResponse()
            .messageList
            .map { unpack(it.state, UserMentions::class.java) }
            .toSet()
    }
}
