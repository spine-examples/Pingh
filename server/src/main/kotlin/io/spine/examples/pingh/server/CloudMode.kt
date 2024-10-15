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

package io.spine.examples.pingh.server

import io.spine.environment.CustomEnvironmentType
import io.spine.examples.pingh.clock.Clock
import io.spine.examples.pingh.github.ClientId
import io.spine.examples.pingh.github.ClientSecret
import io.spine.examples.pingh.github.GitHubApp
import io.spine.examples.pingh.github.of
import io.spine.examples.pingh.server.datastore.DatastoreStorageFactory
import io.spine.server.ServerEnvironment
import io.spine.server.delivery.Delivery
import io.spine.server.transport.memory.InMemoryTransportFactory

/**
 * Google Cloud Platform environment.
 *
 * If the application is running in this mode,
 * it indicates that it has access to Google Cloud services.
 */
internal class CloudMode : CustomEnvironmentType<CloudMode>() {
    /**
     * Returns `true` if the Google Cloud project ID is present in the environment variables.
     */
    override fun enabled(): Boolean = System.getProperty("GCP_PROJECT_ID") != null

    /**
     * Returns cloud mode type instance.
     */
    override fun self(): CloudMode = this
}

/**
 * The server side of the Pingh application, running in the cloud.
 *
 * During the initialization, performs the actions as follows.
 *
 * 1. Configures the server environment for production use,
 * including the interaction with GitHub API and Google Datastore.
 *
 * 2. Starts an [HTTP endpoint][startHeartbeatServer] receiving the current time values
 * from an external clock or a system scheduler.
 */
internal class CloudApplication : Application() {
    /**
     * Obtains secrets of the Pingh GitHub App required for the authentication flow
     * from the Secret Manager.
     */
    override fun gitHubApp() = GitHubApp::class.of(
        ClientId::class.of(Secret.named("github_client_id")),
        ClientSecret::class.of(Secret.named("github_client_secret"))
    )

    /**
     * Configures the server environment.
     *
     * Application data is stored using Google Cloud Datastore. Therefore, any changes made
     * by users of this application will be persisted in-between the application launches.
     */
    override fun configureEnvironment() {
        ServerEnvironment
            .`when`(CloudMode::class.java)
            .use(DatastoreStorageFactory.remote())
            .use(Delivery.localAsync())
            .use(InMemoryTransportFactory.newInstance())
    }

    /**
     * Starts a [server][startHeartbeatServer] to handle HTTP requests that receive
     * the current time from an external clock or system scheduler.
     */
    override fun startClock() {
        startHeartbeatServer(Clock())
    }
}
