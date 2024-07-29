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
import io.spine.examples.pingh.mentions.GitHubClientServiceImpl
import io.spine.examples.pingh.mentions.newMentionsContext
import io.spine.server.Server
import io.spine.server.ServerEnvironment
import io.spine.server.delivery.Delivery
import io.spine.server.storage.memory.InMemoryStorageFactory
import io.spine.server.transport.memory.InMemoryTransportFactory
import io.spine.client.ConnectionConstants.DEFAULT_CLIENT_SERVICE_PORT
import io.spine.examples.pingh.sessions.GitHubAuthenticationServiceImpl
import io.spine.examples.pingh.sessions.newSessionsContext

/**
 * The client ID for the Pingh GitHub App.
 */
// TODO:2024-07-29:mykyta.pimonov: Add a key load from Google Secret Manager
//  after deployment to Google Cloud..
private val clientId = ""

/**
 * The entry point of the server application.
 */
public fun main() {
    val server = createServer()
    server.start()
    server.awaitTermination()
}

/**
 * Creates a new Spine `Server` instance at the
 * [DEFAULT_CLIENT_SERVICE_PORT].
 *
 * The server includes bounded contexts of [Sessions][io.spine.examples.pingh.sessions]
 * and [Mentions][io.spine.examples.pingh.mentions].
 */
private fun createServer(): Server {
    configureEnvironment()
    return Server
        .atPort(DEFAULT_CLIENT_SERVICE_PORT)
        .add(newSessionsContext(GitHubAuthenticationServiceImpl(clientId, CIO.create())))
        .add(newMentionsContext(GitHubClientServiceImpl(CIO.create())))
        .build()
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
