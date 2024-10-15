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
import io.spine.examples.pingh.clock.IntervalClock
import io.spine.examples.pingh.github.ClientId
import io.spine.examples.pingh.github.ClientSecret
import io.spine.examples.pingh.github.GitHubApp
import io.spine.examples.pingh.github.of
import io.spine.server.ServerEnvironment
import io.spine.server.delivery.Delivery
import io.spine.server.storage.memory.InMemoryStorageFactory
import io.spine.server.transport.memory.InMemoryTransportFactory
import java.util.Properties
import kotlin.time.Duration.Companion.seconds

/**
 * The type of environment for local use.
 *
 * When the application is running in this mode,
 * it utilizes resources available on the current machine.
 */
internal class LocalMode : CustomEnvironmentType<LocalMode>() {
    /**
     * Returns `true` if the Google Cloud project ID is absent from the environment variables.
     */
    override fun enabled(): Boolean = System.getProperty("GCP_PROJECT_ID") == null

    /**
     * Returns local mode type instance.
     */
    override fun self(): LocalMode = this
}

/**
 * The server side of the Pingh application for local use.
 *
 * During the initialization, performs the actions as follows.
 *
 * 1. Configures the environment for local use, which includes loading GitHub App secrets
 * from the configuration file.
 *
 * 2. Initializes an [IntervalClock] to emit an event with the current time to the server
 * every second.
 *
 * 3. Builds and launches the Pingh server.
 */
internal class LocalApplication : Application() {
    internal companion object {
        /**
         * Path to the configuration file containing the GitHub App secrets.
         */
        private const val gitHubAppSecretPath = "/local/config/server.properties"
    }

    /**
     * Loads GitHub application secrets from resource folder.
     */
    override fun gitHubApp(): GitHubApp {
        val properties = Properties()
        LocalApplication::class.java.getResourceAsStream(gitHubAppSecretPath).use {
            properties.load(it)
        }
        return GitHubApp::class.of(
            ClientId::class.of(properties.getOrThrow("github-app.client.id")),
            ClientSecret::class.of(properties.getOrThrow("github-app.client.secret"))
        )
    }

    /**
     * Returns the value of an environment variable by its `key` if it exists;
     * otherwise, an [IllegalStateException] is thrown.
     */
    private fun Properties.getOrThrow(key: String): String =
        getProperty(key) ?: throw IllegalStateException(
            "For running Pingh server locally the \"$key\" must be provided " +
                    "in the configuration file located at \"resource$gitHubAppSecretPath\"."
        )

    /**
     * Configures the server environment.
     *
     * Server side of this application is currently running in in-memory storage mode.
     * Therefore, any changes made by users of this application will not be persisted
     * in-between the application launches.
     */
    override fun configureEnvironment() {
        ServerEnvironment
            .`when`(LocalMode::class.java)
            .use(InMemoryStorageFactory.newInstance())
            .use(Delivery.localAsync())
            .use(InMemoryTransportFactory.newInstance())
    }

    /**
     * Starts a [clock][IntervalClock] to emit an event with the current time
     * to the server every second.
     */
    override fun startClock() {
        IntervalClock(1.seconds)
            .start()
    }
}
