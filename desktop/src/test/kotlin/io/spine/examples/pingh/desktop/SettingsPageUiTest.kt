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

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import io.spine.examples.pingh.desktop.given.DelayedFactAssertion.Companion.awaitFact
import kotlin.test.Test
import org.junit.jupiter.api.DisplayName

@DisplayName("Settings page should")
@OptIn(ExperimentalTestApi::class)
internal class SettingsPageUiTest : UiTest() {

    private val SemanticsNodeInteractionsProvider.settingsButton
        get() = onNodeWithTag("settings-button")

    private val SemanticsNodeInteractionsProvider.logoutButton
        get() = onNodeWithTag("logout-button")

    private val SemanticsNodeInteractionsProvider.dndOption
        get() = onNodeWithTag("dnd-option")

    @Test
    internal fun `have settings retained after the user logs in again`() =
        runComposeUiTest {
            runApp()
            logIn()
            awaitFact { settingsButton.assertExists() }
            settingsButton.performClick()
            awaitFact { dndOption.assertExists() }
            dndOption.performClick()
            awaitFact { dndOption.assertIsOn() }
            logoutButton.performClick()
            awaitFact { logoutButton.assertDoesNotExist() }
            logIn()
            awaitFact { settingsButton.assertExists() }
            settingsButton.performClick()
            awaitFact { dndOption.assertIsOn() }
        }
}
