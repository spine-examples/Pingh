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

import io.spine.environment.Tests
import io.spine.examples.pingh.client.VerifyLogin
import io.spine.examples.pingh.client.PinghApplication
import io.spine.examples.pingh.client.e2e.given.MemoizingNotificationSender
import io.spine.examples.pingh.clock.IntervalClock
import io.spine.examples.pingh.mentions.newMentionsContext
import io.spine.examples.pingh.server.datastore.DatastoreStorageFactory
import io.spine.examples.pingh.sessions.newSessionsContext
import io.spine.examples.pingh.testing.mentions.given.PredefinedGitHubSearchResponses
import io.spine.examples.pingh.testing.sessions.given.PredefinedGitHubAuthenticationResponses
import io.spine.server.Server
import io.spine.server.ServerEnvironment
import kotlin.time.Duration.Companion.milliseconds
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach

/**
 * Abstract base for tests that need to connect to a [Server].
 *
 * Also provides a [PinghApplication] for interacting with the `Server`.
 */
internal abstract class IntegrationTest {

    internal companion object {

        private const val port = 4242
        private const val address = "localhost"

        private val storage = DatastoreStorageFactory.local()
        private val auth = PredefinedGitHubAuthenticationResponses()
        private val search = PredefinedGitHubSearchResponses()
        private val clock = IntervalClock(100.milliseconds)

        private lateinit var server: Server

        @BeforeAll
        @JvmStatic
        internal fun startServer() {
            configureEnvironment()
            server = createServer()
            server.start()
        }

        /**
         * Configures the server environment.
         */
        private fun configureEnvironment() {
            ServerEnvironment.`when`(Tests::class.java)
                .use(storage)
        }

        /**
         * Creates a new test Pingh `Server`.
         */
        private fun createServer(): Server =
            Server.atPort(port)
                .add(newSessionsContext(auth))
                .add(newMentionsContext(search))
                .build()

        @AfterAll
        @JvmStatic
        internal fun shutdownServer() {
            clock.stop()
            server.shutdown()
            ServerEnvironment.instance().reset()
        }
    }

    private lateinit var notificationSender: MemoizingNotificationSender
    private lateinit var application: PinghApplication

    @BeforeEach
    internal fun createApplication() {
        clock.start()
        notificationSender = MemoizingNotificationSender()
        application = PinghApplication(notificationSender, address, port)
    }

    @AfterEach
    internal fun clearDataFromPreviousTest() {
        clock.stop()
        application.close()
        auth.reset()
        search.reset()
        storage.clear()
    }

    /**
     * Returns the `PinghApplication` connected to the server.
     */
    protected fun app(): PinghApplication = application

    /**
     * Marks that the user has entered their user code.
     *
     * After calling this method, the login verification will be successful,
     * which will allow to use login to the application after calling
     * the [VerifyLogin.confirm] method.
     */
    protected fun enterUserCode() {
        auth.enterUserCode()
    }

    /**
     * Returns the count of sent notifications.
     */
    protected fun notificationsCount(): Int = notificationSender.notificationsCount()
}
