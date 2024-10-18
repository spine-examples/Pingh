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

package io.spine.examples.pingh.desktop.given

import androidx.compose.runtime.remember
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly
import io.spine.examples.pingh.client.TestClient
import io.spine.examples.pingh.desktop.AppState
import io.spine.examples.pingh.desktop.Theme
import io.spine.examples.pingh.desktop.Window
import io.spine.examples.pingh.desktop.loadServerProperties
import io.spine.examples.pingh.desktop.retrieveSystemSettings
import java.util.concurrent.TimeUnit

/**
 * Launches the Pingh application for testing.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.runApp() {
    setContent {
        Theme {
            val settings = retrieveSystemSettings()
            val state = remember { AppState(settings) }
            Window(state.window, state.app)
        }
    }
}

/**
 * Causes the currently executing thread to sleep for the specified number of milliseconds
 *
 * Compose uses coroutines to update UI elements, which means that updates
 * may not occur immediately after a state change. Therefore, it is advisable to introduce
 * a brief delay before checking for state updates.
 */
internal fun delay(millis: Long = 100) {
    sleepUninterruptibly(millis, TimeUnit.MILLISECONDS)
}

/**
 * Loads connection data for the server and creates a test client to interact with it.
 */
internal fun createTestClient(): TestClient {
    val properties = loadServerProperties()
    return TestClient(
        properties.getProperty("server.address"),
        properties.getProperty("server.port").toInt()
    )
}
