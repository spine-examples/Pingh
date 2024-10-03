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

import com.google.cloud.datastore.DatastoreOptions
import io.spine.server.storage.datastore.DatastoreStorageFactory
import io.spine.server.storage.datastore.DsColumnMapping
import io.spine.server.storage.datastore.ProjectId
import io.spine.testing.server.storage.datastore.TestDatastoreStorageFactory

/**
 * A factory for Datastore storages.
 */
public object DatastoreStorageFactory {

    /**
     * The default port to which the local Datastore emulator is bound.
     */
    private const val defaultLocalPort = 8081

    /**
     * The default project ID to use when running on a local Datastore emulator.
     */
    private val defaultLocalProjectId = ProjectId.of("test-project")

    /**
     * Creates a new `TestDatastoreStorageFactory` instance that works with
     * a local Datastore emulator running in a Docker container.
     */
    public fun local(): TestDatastoreStorageFactory {
        val datastore = Emulator
            .at(defaultLocalProjectId, defaultLocalPort)
            .service
        return TestDatastoreStorageFactory.basedOn(datastore)
    }

    /**
     * Creates a factory for generating `Storage`s based on remote Google Cloud Datastore.
     *
     * Uses a Datastore named `(default)`.
     *
     * To connect to the Datastore, the [project ID][projectId] must be specified as
     * a system parameter with the key `GCP_PROJECT_ID`.
     */
    public fun remote(): DatastoreStorageFactory {
        val datastore = DatastoreOptions.newBuilder()
            .setProjectId(projectId())
            .build()
            .service
        return DatastoreStorageFactory.newBuilder()
            .setDatastore(datastore)
            .setColumnMapping(DsColumnMapping())
            .build()
    }

    /**
     * Returns the Google Cloud Platform project ID from system properties.
     *
     * @throws IllegalStateException if `GCP_PROJECT_ID` property is empty.
     */
    private fun projectId(): String =
        System.getProperty("GCP_PROJECT_ID") ?: throw IllegalStateException(
            "The project ID is not specified as a system property using the `GCP_PROJECT_ID` key."
        )
}
