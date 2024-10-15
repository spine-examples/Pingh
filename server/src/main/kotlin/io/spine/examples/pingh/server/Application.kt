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
import io.spine.environment.Environment
import io.spine.examples.pingh.github.GitHubApp
import io.spine.examples.pingh.mentions.RemoteGitHubSearch
import io.spine.examples.pingh.mentions.newMentionsContext
import io.spine.examples.pingh.sessions.RemoteGitHubAuthentication
import io.spine.examples.pingh.sessions.RemoteGitHubUsers
import io.spine.examples.pingh.sessions.newSessionsContext
import io.spine.server.Server

/**
 * The server side of the Pingh application.
 */
@Suppress("LeakingThis" /* Abstract method implementations are required to create server */)
internal abstract class Application {

    internal companion object {
        /**
         * The port on which the Pingh server runs.
         */
        private const val pinghPort = 50051

        /**
         * Returns a [CloudApplication] if the app is running on Google Cloud Platform;
         * otherwise, returns [LocalApplication].
         */
        internal fun newInstance(): Application =
            if (Environment.instance().`is`(CloudMode::class.java)) {
                CloudApplication()
            } else {
                LocalApplication()
            }
    }

    /**
     * The Pingh server.
     */
    internal val server: Server

    init {
        configureEnvironment()
        startClock()
        server = createServer()
    }

    /**
     * Returns the GitHub App secrets required to make authentication requests
     * on behalf of the App.
     */
    protected abstract fun gitHubApp(): GitHubApp

    /**
     * Configures the server environment.
     */
    protected abstract fun configureEnvironment()

    /**
     * Starts emitting periodic events with the current time to the server.
     */
    protected abstract fun startClock()

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
                    RemoteGitHubAuthentication(gitHubApp(), httpEngine),
                    RemoteGitHubUsers(httpEngine)
                )
            )
            .add(newMentionsContext(RemoteGitHubSearch(httpEngine)))
            .build()
    }
}
