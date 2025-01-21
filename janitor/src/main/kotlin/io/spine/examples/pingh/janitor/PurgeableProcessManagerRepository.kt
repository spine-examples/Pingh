/*
 * Copyright 2025, TeamDev. All rights reserved.
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

package io.spine.examples.pingh.janitor

import io.spine.base.EntityState
import io.spine.client.CompositeFilter
import io.spine.client.CompositeFilter.CompositeOperator.EITHER
import io.spine.client.Filters
import io.spine.client.TargetFilters
import io.spine.logging.Logging
import io.spine.protobuf.AnyPacker
import io.spine.server.entity.storage.EntityQueries
import io.spine.server.procman.ProcessManager
import io.spine.server.procman.ProcessManagerRepository

/**
 * Abstract base for purgeable process manager repositories,
 * providing an implemented method to [purge] the repository
 * of records marked as archived or deleted.
 *
 * @param I The type of IDs of process managers.
 * @param P The type of process managers.
 * @param S The type of process manager state.
 */
public abstract class PurgeableProcessManagerRepository<I,
        P : ProcessManager<I, S, *>,
        S : EntityState>
    : ProcessManagerRepository<I, P, S>(), Purgeable, Logging {

    /**
     * Physically removes entity records marked as archived or deleted from the storage.
     */
    public override fun purge() {
        val storage = recordStorage()
        val archivedOrDeleted = CompositeFilter.newBuilder()
            .setOperator(EITHER)
            .addFilter(Filters.eq("archived", true))
            .addFilter(Filters.eq("deleted", true))
            .vBuild()
        val targetFilter = TargetFilters.newBuilder()
            .addFilter(archivedOrDeleted)
            .vBuild()
        val query = EntityQueries.from(targetFilter, storage)

        val obsoleteRecords = storage.readAll(query)
        var deleted = 0
        for (record in obsoleteRecords) {
            @Suppress("UNCHECKED_CAST" /* Entity ID is defined in the class declaration. */)
            val id = AnyPacker.unpack(record.entityId) as I
            storage.delete(id!!)
            deleted++
        }
        _debug().log(
            "$deleted obsolete records of \"${entityClass().name}\" were removed from the storage."
        )
    }
}
