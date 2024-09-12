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
import io.spine.server.storage.datastore.ProjectId
import org.testcontainers.utility.DockerImageName

/**
 * Manages Datastore emulators running in Docker containers.
 */
internal object Emulator {

    /**
     * The name of the Docker `google-cloud-cli` image.
     */
    private val image: DockerImageName = DockerImageName
        .parse("gcr.io/google.com/cloudsdktool/google-cloud-cli:441.0.0-emulators")

    /**
     * Currently running [emulator containers][EmulatorContainer],
     * along with the project ID each is associated with.
     */
    private val containers: MutableMap<ProjectId, EmulatorContainer> = mutableMapOf()

    /**
     * Returns the connection options to the running Datastore emulator.
     *
     * If an emulator for this project is already running, it returns the connection options
     * to that emulator. Otherwise, it creates a new emulator container via Docker and
     * returns its connection options.
     *
     * @param id The ID of the project interacting with the Datastore emulator.
     * @param port The port used to access the Datastore emulator within
     *   the Docker container machine.
     */
    internal fun at(id: ProjectId, port: Int): DatastoreOptions {
        var emulator = containers[id]
        if (emulator == null) {
            emulator = EmulatorContainer(image, id, port)
            emulator.startAndServe()
            containers[id] = emulator
        }
        return emulator.options
    }
}
