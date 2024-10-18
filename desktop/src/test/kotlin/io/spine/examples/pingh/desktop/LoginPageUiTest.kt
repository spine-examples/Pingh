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

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import io.spine.examples.pingh.desktop.given.createTestClient
import io.spine.examples.pingh.desktop.given.delay
import io.spine.examples.pingh.desktop.given.runApp
import io.spine.examples.pingh.sessions.event.UserCodeReceived
import io.spine.examples.pingh.testing.client.IntegrationTest
import kotlin.test.Test
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.DisplayName

@DisplayName("Login page should")
@OptIn(ExperimentalTestApi::class)
internal class LoginPageUiTest : IntegrationTest() {
    internal companion object {
        private val client = createTestClient()

        @AfterAll
        @JvmStatic
        internal fun closeClient() {
            client.close()
        }
    }

    private val username = "MykytaPimonovTD"

    private val SemanticsNodeInteractionsProvider.loginButton
        get() = onNodeWithTag("login-button")

    private val SemanticsNodeInteractionsProvider.usernameInput
        get() = onNodeWithTag("username-input")

    private val SemanticsNodeInteractionsProvider.submitButton
        get() = onNodeWithTag("submit-button")

    private val SemanticsNodeInteractionsProvider.noResponseMessage
        get() = onNodeWithTag("no-response-message")

    @Test
    internal fun `have login button enabled only when a valid username is entered`() =
        runComposeUiTest {
            runApp()
            loginButton.assertIsNotEnabled()
            usernameInput.performTextInput("()+$")
            loginButton.assertIsNotEnabled()
            usernameInput.performTextClearance()
            usernameInput.performTextInput(username)
            delay()
            loginButton.assertIsEnabled()
        }

    @Test
    internal fun `have submit button disabled after it is clicked, if no code has been entered`() =
        runComposeUiTest {
            runApp()
            toVerificationPage()
            submitButton.assertIsEnabled()
            noResponseMessage.assertDoesNotExist()
            submitButton.performClick()
            delay()
            submitButton.assertIsNotEnabled()
            noResponseMessage.assertExists()
        }

    @Test
    internal fun `have submit button become available again 5 seconds after unsuccessful click`() =
        runComposeUiTest {
            runApp()
            toVerificationPage()
            submitButton.performClick()
            delay(5100)
            submitButton.assertIsEnabled()
            noResponseMessage.assertDoesNotExist()
        }

    private fun ComposeUiTest.toVerificationPage() {
        usernameInput.performTextInput(username)
        delay()
        val observer = client.observeEvent(UserCodeReceived::class)
        loginButton.performClick()
        observer.waitUntilDone()
        delay()
        loginButton.assertDoesNotExist()
        submitButton.assertExists()
    }
}
