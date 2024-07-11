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

package io.spine.examples.pingh.client.e2e

import io.spine.examples.pingh.client.DesktopClient
import io.spine.examples.pingh.clock.IntervalClock
import io.spine.examples.pingh.testing.mentions.given.PredefinedGitHubResponses
import io.spine.examples.pingh.mentions.newMentionsContext
import io.spine.examples.pingh.sessions.newSessionsContext
import io.spine.server.Server
import kotlin.time.Duration.Companion.milliseconds
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

/**
 * Abstract base for tests that need to connect to a [Server].
 *
 * Also provides a [DesktopClient] for interacting with the `Server`.
 */
internal abstract class IntegrationTest {

    private val port = 4242
    private val address = "localhost"
    private lateinit var clock: IntervalClock
    private lateinit var server: Server
    private lateinit var client: DesktopClient

    @BeforeEach
    internal fun runServer() {
        clock = IntervalClock(100.milliseconds)
        clock.start()
        server = createServer(port)
        server.start()
        client = DesktopClient(address, port)
    }

    @AfterEach
    internal fun shutdownServer() {
        client.close()
        clock.stop()
        server.shutdown()
    }

    /**
     * Returns the `DesktopClient` connected to the server.
     */
    protected fun client(): DesktopClient = client

    private companion object {
        /**
         * Creates a new test Pingh `Server`.
         */
        private fun createServer(port: Int): Server =
            Server.atPort(port)
                .add(newSessionsContext())
                .add(newMentionsContext(PredefinedGitHubResponses()))
                .build()
    }
}
