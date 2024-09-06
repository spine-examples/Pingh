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

import com.google.cloud.NoCredentials
import com.google.cloud.ServiceOptions
import com.google.cloud.datastore.DatastoreOptions
import io.spine.server.storage.datastore.ProjectId
import org.testcontainers.containers.DatastoreEmulatorContainer
import org.testcontainers.utility.DockerImageName

/**
 * A container running the Datastore emulator image that relies on the Google Cloud SDK.
 *
 * The container is started in "full consistency" mode.
 *
 * @param name The name of the Docker `google-cloud-sdk` or `google-cloud-cli` image.
 * @property projectId The ID of the project interacting with the Datastore emulator.
 * @property port The port on which the emulator will be exposed.
 */
internal class EmulatorContainer internal constructor(
    name: DockerImageName,
    private val projectId: ProjectId,
    private val port: Int
) : DatastoreEmulatorContainer(name) {

    /**
     * Template of a BASH command that starts a local Datastore emulator.
     *
     * This command is executed when the Docker container starts.
     *
     * @see <a href="https://cloud.google.com/sdk/gcloud/reference/beta/emulators/datastore/start">
     *     Start Datastore emulator command</a>
     */
    private val cmdTemplate: String = "gcloud beta emulators datastore start " +
            "--project %s " +
            "--host-port 0.0.0.0:%d " +
            "--consistency 1.0"

    /**
     * Datastore options of the running emulator.
     *
     * Initialized upon the invocation of [startAndServe()][startAndServe].
     */
    internal lateinit var options: DatastoreOptions
        private set

    /**
     * Starts the Docker container and returns the connection options
     * for the local Datastore emulator.
     */
    internal fun startAndServe(): DatastoreOptions {
        super.start()
        options = DatastoreOptions.newBuilder()
            .setHost(getEmulatorEndpoint())
            .setCredentials(NoCredentials.getInstance())
            .setRetrySettings(ServiceOptions.getNoRetrySettings())
            .setProjectId(getProjectId())
            .build()
        return options
    }

    /**
     * Returns the `host:pair` representing the address where the emulator
     * can be accessed from the test host machine.
     */
    override fun getEmulatorEndpoint(): String = "$host:${getMappedPort(port)}"

    /**
     * Returns the ID of the project interacting with the Datastore emulator.
     */
    override fun getProjectId(): String = projectId.value

    /**
     * Executes the command to start the Datastore emulator during the container's configuration.
     *
     * @see [cmdTemplate]
     */
    override fun configure() {
        val command = cmdTemplate.format(getProjectId(), port)
        withCommand("/bin/sh", "-c", command)
    }
}
