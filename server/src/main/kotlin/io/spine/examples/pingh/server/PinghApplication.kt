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

import io.ktor.client.engine.cio.CIO
import io.spine.environment.DefaultMode
import io.spine.examples.pingh.clock.Clock
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
import io.spine.server.transport.memory.InMemoryTransportFactory

/**
 * The server side of the Pingh application.
 *
 * During the initialization, performs the actions as follows.
 *
 * 1. Configures the server environment for production use,
 * including the interaction with GitHub API, Google Datastore, etc.
 *
 * 2. Starts an [HTTP endpoint][startHeartbeatServer] receiving the current time values
 * from an external clock or a system scheduler.
 */
internal class PinghApplication {
    private companion object {
        /**
         * The port on which the Pingh server runs.
         */
        private const val pinghPort = 50051

        /**
         * Secrets of the Pingh GitHub App required for the authentication flow.
         */
        private val gitHubApp = GitHubApp::class.of(
            ClientId::class.of(Secret.named("github_client_id")),
            ClientSecret::class.of(Secret.named("github_client_secret"))
        )
    }

    /**
     * The Pingh server.
     */
    internal val server: Server

    init {
        configureEnvironment()
        server = createServer()
        startHeartbeatServer(Clock())
    }

    /**
     * Configures the server environment.
     *
     * Application data is stored using Google Cloud Datastore. Therefore, any changes made
     * by users of this application will be persisted in-between the application launches.
     */
    private fun configureEnvironment() {
        ServerEnvironment
            .`when`(DefaultMode::class.java)
            .use(DatastoreStorageFactory.remote())
            .use(Delivery.localAsync())
            .use(InMemoryTransportFactory.newInstance())
    }

    /**
     * Creates a new Spine `Server` instance at the [pinghPort].
     *
     * The server includes bounded contexts of [Sessions][io.spine.examples.pingh.sessions]
     * and [Mentions][io.spine.examples.pingh.mentions].
     */
    private fun createServer(): Server {
        val httpEngine = CIO.create()
        return Server
            .atPort(pinghPort)
            .add(
                newSessionsContext(
                    RemoteGitHubAuthentication(gitHubApp, httpEngine),
                    RemoteGitHubUsers(httpEngine)
                )
            )
            .add(newMentionsContext(RemoteGitHubSearch(httpEngine)))
            .build()
    }
}
