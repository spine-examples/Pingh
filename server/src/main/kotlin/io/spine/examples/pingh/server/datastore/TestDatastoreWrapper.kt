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

package io.spine.examples.pingh.server.datastore

import com.google.cloud.datastore.Datastore
import com.google.cloud.datastore.KeyFactory
import io.spine.server.storage.datastore.DatastoreWrapper
import io.spine.server.storage.datastore.Kind
import io.spine.server.storage.datastore.tenant.NamespaceSupplier

/**
 * A custom extension of the [DatastoreWrapper] for the integration testing.
 *
 * @param datastore The `Datastore` to wrap.
 */
internal class TestDatastoreWrapper(datastore: Datastore) :
    DatastoreWrapper(datastore, NamespaceSupplier.singleTenant()) {

    /**
     * List of entity [kinds][Kind] stored in the Datastore.
     */
    private val kinds: MutableList<Kind> = mutableListOf()

    /**
     * Retrieves an instance of `KeyFactory` unique for given [Kind]
     * of data regarding the current namespace and caches it.
     */
    override fun keyFactory(kind: Kind): KeyFactory {
        kinds.add(kind)
        return super.keyFactory(kind)
    }

    /**
     * Deletes all records from the Datastore.
     */
    internal fun dropAllTables() {
        kinds.forEach { dropTable(it) }
        kinds.clear()
    }
}
