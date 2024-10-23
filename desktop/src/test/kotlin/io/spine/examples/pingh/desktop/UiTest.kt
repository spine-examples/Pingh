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

package io.spine.examples.pingh.desktop

import androidx.compose.runtime.remember
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import io.spine.examples.pingh.desktop.given.MemoizingUriHandler
import io.spine.examples.pingh.testing.client.IntegrationTest
import org.junit.jupiter.api.AfterEach

/**
 * Abstract base for UI tests that require a server to run and
 * a client application connected to it.
 */
internal abstract class UiTest : IntegrationTest() {

    private var state: AppState? = null
    private val uriHandler = MemoizingUriHandler()

    @AfterEach
    internal fun shutdownChannel() {
        state?.app?.close()
        uriHandler.reset()
    }

    /**
     * Launches the Pingh application for testing and sets application state for this test case.
     */
    @OptIn(ExperimentalTestApi::class)
    protected fun ComposeUiTest.runApp() {
        setContent {
            Theme {
                val settings = retrieveSystemSettings()
                state = remember { AppState(settings) }
                Window(state!!.window, state!!.app, uriHandler)
            }
        }
    }

    /**
     * Returns the number of opened URLs.
     *
     * To correctly count opened links, the test application
     * must be executed using [runApp()][ComposeUiTest.runApp] method.
     */
    protected fun openedUrlCount(): Int = uriHandler.urlCount
}
