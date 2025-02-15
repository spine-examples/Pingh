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

package io.spine.examples.pingh.server

import io.ktor.client.engine.cio.CIO
import io.spine.environment.DefaultMode
import io.spine.environment.Environment
import io.spine.examples.pingh.clock.Clock
import io.spine.examples.pingh.clock.LocalIntervalClock
import io.spine.examples.pingh.github.ClientId
import io.spine.examples.pingh.github.ClientSecret
import io.spine.examples.pingh.github.GitHubApp
import io.spine.examples.pingh.github.of
import io.spine.examples.pingh.mentions.RemoteGitHubSearch
import io.spine.examples.pingh.mentions.newMentionsContext
import io.spine.examples.pingh.server.datastore.DatastoreStorageFactory
import io.spine.examples.pingh.sessions.RemoteGitHubAuthentication
import io.spine.examples.pingh.sessions.RemoteGitHubUsers
import io.spine.examples.pingh.sessions.newSessionsContext
import io.spine.server.Server
import io.spine.server.ServerEnvironment
import io.spine.server.delivery.Delivery
import io.spine.server.storage.memory.InMemoryStorageFactory
import io.spine.server.transport.memory.InMemoryTransportFactory
import java.util.Properties
import kotlin.time.Duration.Companion.seconds

/**
 * The server side of the Pingh application.
 *
 * The configuration of the application varies depending on
 * the environment in which it is running.
 *
 * In [Production] mode, the following actions are performed during initialization:
 *
 * 1. Configures the server environment for production use,
 * including the interaction with GitHub API and Google Datastore.
 *
 * 2. Starts an [HTTP endpoint][HeartbeatServer] receiving the current time values
 * from an external clock or a system scheduler.
 *
 * In non-production mode, the following actions are performed during initialization:
 *
 * 1. Configures the environment for local use, including loading
 * GitHub App secrets from the configuration file.
 *
 * 2. Initializes an [LocalIntervalClock] to emit an event with the current time
 * to the server every second.
 */
internal class Application {

    private val server: Server
    private val clock: Clock
    init {
        configureEnvironment()
        server = createServer()
        clock = createClock()
    }

    /**
     * Configures the server environment.
     */
    private fun configureEnvironment() {
        ServerEnvironment.`when`(DefaultMode::class.java)
            .useStorageFactory { InMemoryStorageFactory.newInstance() }
            .useDelivery { Delivery.localAsync() }
            .useTransportFactory { InMemoryTransportFactory.newInstance() }

        ServerEnvironment.`when`(Production::class.java)
            .useStorageFactory { DatastoreStorageFactory.remote() }
            .useDelivery { Delivery.localAsync() }
            .useTransportFactory { InMemoryTransportFactory.newInstance() }
    }

    /**
     * Creates a clock to send current time to the server.
     *
     * In [Production] mode, creates a [server][HeartbeatServer]
     * to handle HTTP requests from an external clock or system scheduler.
     * In non-production mode, creates a [clock][LocalIntervalClock]
     * to emit an event to the server every second.
     */
    private fun createClock(): Clock =
        if (isProduction()) {
            HeartbeatServer()
        } else {
            LocalIntervalClock(1.seconds)
        }

    /**
     * Return `true` if the application is running in [Production] mode.
     */
    private fun isProduction(): Boolean = Environment.instance().`is`(Production::class.java)

    /**
     * Creates a new Spine `Server` instance at the [port].
     *
     * The server includes bounded contexts of [Sessions][io.spine.examples.pingh.sessions]
     * and [Mentions][io.spine.examples.pingh.mentions].
     */
    private fun createServer(): Server {
        val httpEngine = CIO.create()
        val auth = RemoteGitHubAuthentication(gitHubApp(), httpEngine)
        val search = RemoteGitHubSearch(httpEngine)
        val users = RemoteGitHubUsers(httpEngine)
        return Server
            .atPort(port)
            .add(newSessionsContext(auth, users))
            .add(newMentionsContext(search, users))
            .build()
    }

    /**
     * Starts the Pingh server and clock.
     */
    internal fun start() {
        server.start()
        clock.start()
    }

    /**
     * Waits for the server to become terminated.
     */
    internal fun awaitTermination() {
        server.awaitTermination()
    }

    internal companion object {
        /**
         * The port on which the Pingh server runs.
         */
        private const val port = 50051
    }
}

/**
 * Returns the GitHub App secrets required to make authentication requests
 * on behalf of the App.
 *
 * In [Production] mode, obtains secrets from the Secret Manager.
 * In non-production mode, loads secrets from resource folder.
 */
private fun gitHubApp(): GitHubApp =
    if (Environment.instance().`is`(Production::class.java)) {
        GitHubApp::class.of(
            ClientId::class.of(Secret.named("github_client_id")),
            ClientSecret::class.of(Secret.named("github_client_secret"))
        )
    } else {
        loadGitHubAppSecrets()
    }

/**
 * Loads GitHub application secrets from resource folder.
 */
private fun loadGitHubAppSecrets(): GitHubApp {
    val properties = Properties()
    val path = "/local/config/server.properties"
    Application::class.java.getResourceAsStream(path).use {
        properties.load(it)
    }
    val messageFormat = "For running Pingh server locally the \"%s\" property must be provided " +
            "in the configuration file located at \"resource$path\"."
    return GitHubApp::class.of(
        ClientId::class.of(properties.getOrThrow("github-app.client.id", messageFormat)),
        ClientSecret::class.of(properties.getOrThrow("github-app.client.secret", messageFormat))
    )
}

/**
 * Returns the value of property by its `key` if it exists;
 * otherwise, an [IllegalStateException] is thrown.
 */
private fun Properties.getOrThrow(key: String, messageFormat: String): String =
    getProperty(key) ?: throw IllegalStateException(messageFormat.format(key))
