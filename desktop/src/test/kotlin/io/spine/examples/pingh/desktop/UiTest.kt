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

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import io.spine.examples.pingh.desktop.given.DelayedFactAssertion.Companion.awaitFact
import io.spine.examples.pingh.desktop.given.MemoizingUriHandler
import io.spine.examples.pingh.testing.client.IntegrationTest
import org.junit.jupiter.api.AfterEach

/**
 * Abstract base for UI tests that require a server to run and
 * a client application connected to it.
 */
internal abstract class UiTest : IntegrationTest() {

    protected val username = "MykytaPimonovTD"
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
    protected fun runPinghUiTest(testBlock: ComposeUiTest.() -> Unit) =
        runDesktopComposeUiTest(
            width = 420,
            height = 700
        ) {
            val serverEndpoint = ServerEndpoint(address, port)
            setContent {
                Theme {
                    state = remember { AppState(serverEndpoint) }
                    CompositionLocalProvider(LocalUriHandler provides uriHandler) {
                        CurrentPage(state!!.app, state!!::close)
                    }
                }
            }
            testBlock()
        }

    /**
     * Returns the number of opened URLs.
     *
     * To correctly count opened URLs, the test application
     * must be run using [runPinghUiTest()][runPinghUiTest] method.
     */
    protected fun openedUrlCount(): Int = uriHandler.urlCount

    /**
     * Returns the count of unread mentions for the user
     * or -1 if the [state] is not specified.
     */
    protected fun unreadMentionCount(): Int = state?.app?.unreadMentionCount?.value ?: -1

    /**
     * Logs the user in the application with the specified username and
     * navigates to the Mentions page.
     */
    @OptIn(ExperimentalTestApi::class)
    protected fun ComposeUiTest.logIn() {
        enterUserCode()
        onNodeWithTag("username-input").performTextInput(username)
        awaitFact { onNodeWithTag("login-button").assertIsEnabled() }
        onNodeWithTag("login-button").performClick()
    }
}
