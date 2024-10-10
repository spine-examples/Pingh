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
import io.spine.examples.pingh.clock.IntervalClock
import io.spine.examples.pingh.github.ClientId
import io.spine.examples.pingh.github.ClientSecret
import io.spine.examples.pingh.github.GitHubApp
import io.spine.examples.pingh.github.of
import io.spine.examples.pingh.mentions.RemoteGitHubSearch
import io.spine.examples.pingh.mentions.newMentionsContext
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
 * The port on which the Pingh server runs.
 */
private const val pinghPort = 50051

/**
 * The entry point for the local server application.
 *
 * Server setup and startup process:
 *
 * 1. Configures the environment for local use, including loading GitHub App secrets
 * from the configuration file.
 *
 * 2. Initializes an [IntervalClock] to emit an event with the current time to the server
 * every second.
 *
 * 3. Builds and launches the Pingh server.
 */
public fun main() {
    configureEnvironment()
    val gitHubApp = gitHubApp()
    val server = createServer(gitHubApp)
    IntervalClock(1.seconds).start()
    server.start()
    server.awaitTermination()
}

/**
 * Configures the server environment.
 *
 * Server side of this application is currently running in in-memory storage mode.
 * Therefore, any changes made by users of this application will not be persisted
 * in-between the application launches.
 */
private fun configureEnvironment() {
    ServerEnvironment
        .`when`(DefaultMode::class.java)
        .use(InMemoryStorageFactory.newInstance())
        .use(Delivery.localAsync())
        .use(InMemoryTransportFactory.newInstance())
}

/**
 * Loads GitHub application properties from resource folder.
 */
private fun gitHubApp(): GitHubApp {
    val properties = Properties()
    val path = "/local/config/server.properties"
    PinghApplication::class.java.getResourceAsStream(path).use {
        properties.load(it)
    }
    check(properties.containsKey("github-app.client.id")) {
        "For running Pingh server locally the GitHub App client ID must be provided " +
                "in the configuration file located at \"resource$path\"."
    }
    check(properties.containsKey("github-app.client.secret")) {
        "For running Pingh server locally the GitHub App client secret must be provided " +
                "in the configuration file located at \"resource$path\"."
    }
    return GitHubApp::class.of(
        ClientId::class.of(properties.getProperty("github-app.client.id")),
        ClientSecret::class.of(properties.getProperty("github-app.client.secret"))
    )
}

/**
 * Creates a new Spine `Server` instance at the [pinghPort].
 *
 * The server includes bounded contexts of [Sessions][io.spine.examples.pingh.sessions]
 * and [Mentions][io.spine.examples.pingh.mentions].
 */
private fun createServer(gitHubApp: GitHubApp): Server {
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
