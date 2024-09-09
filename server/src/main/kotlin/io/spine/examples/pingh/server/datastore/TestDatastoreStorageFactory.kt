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
import io.spine.server.storage.datastore.DatastoreStorageFactory
import io.spine.server.storage.datastore.DatastoreWrapper
import io.spine.server.storage.datastore.DsColumnMapping
import io.spine.server.storage.datastore.ProjectId

/**
 * A test implementation of the [DatastoreStorageFactory].
 *
 * @param datastore The `Datastore` for working with data.
 */
public class TestDatastoreStorageFactory private constructor(datastore: Datastore) :
    DatastoreStorageFactory(newBuilderWithDefaults(datastore)) {

    public companion object {

        /**
         * The default port to which the local Datastore emulator is bound.
         */
        private const val port = 8081

        /**
         * The default project ID to use when running on a local Datastore emulator.
         */
        private val defaultLocalProjectId = ProjectId.of("test-project")

        /**
         * Creates a new TestDatastoreStorageFactory instance that works with
         * a local Datastore emulator running in a Docker container.
         */
        public fun local(): TestDatastoreStorageFactory {
            val datastore = Emulator
                .at(defaultLocalProjectId, port)
                .service
            return TestDatastoreStorageFactory(datastore)
        }
    }

    /**
     * Set of `wrappers` created for this storage factory.
     */
    private val wrappers: MutableSet<TestDatastoreWrapper> = mutableSetOf()

    /**
     * Creates an instance of `DatastoreWrapper` and saves it.
     */
    override fun createDatastoreWrapper(multitenant: Boolean): DatastoreWrapper {
        val wrapper = TestDatastoreWrapper(datastore())
        wrappers.add(wrapper)
        return wrapper
    }

    /**
     * Returns the currently known initialized `DatastoreWrapper`s.
     */
    override fun wrappers(): MutableIterable<DatastoreWrapper> = wrappers.toMutableSet()

    /**
     * Clears all data in the local Datastore.
     *
     * Note that this does not stop the server.
     */
    public fun clear() {
        wrappers.forEach { it.dropAllTables() }
    }
}

/**
 * Creates a new instance of `Builder`, passing the `Datastore` to it,
 * and configuring the builder with default settings.
 */
private fun newBuilderWithDefaults(datastore: Datastore): DatastoreStorageFactory.Builder =
    DatastoreStorageFactory.newBuilder()
        .setDatastore(datastore)
        .setColumnMapping(DsColumnMapping())
