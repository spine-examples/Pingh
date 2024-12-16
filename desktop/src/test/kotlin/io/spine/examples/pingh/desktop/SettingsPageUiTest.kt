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
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelectable
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.spine.examples.pingh.desktop.given.DelayedFactAssertion.Companion.awaitFact
import kotlin.test.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@DisplayName("Settings page should")
@OptIn(ExperimentalTestApi::class)
internal class SettingsPageUiTest : UiTest() {

    private val SemanticsNodeInteractionsProvider.menuButton
        get() = onNodeWithTag("menu-button")

    private val SemanticsNodeInteractionsProvider.settingsButton
        get() = onNodeWithTag("settings-button")

    private val SemanticsNodeInteractionsProvider.logoutButton
        get() = onNodeWithTag("logout-button")

    private val SemanticsNodeInteractionsProvider.dndOption
        get() = onNodeWithTag("dnd-option")

    private val SemanticsNodeInteractionsProvider.snoozeTimeOption
        get() = onNodeWithTag("snooze-time-option")

    private val SemanticsNodeInteractionsProvider.addButton
        get() = onNodeWithTag("add-button")

    private val SemanticsNodeInteractionsProvider.removeButton
        get() = onNodeWithTag("remove-button")

    private val SemanticsNodeInteractionsProvider.ignoredSources
        get() = onNodeWithTag("ignored-sources")

    private val SemanticsNodeInteractionsProvider.orgField
        get() = onNodeWithTag("org-field")

    private val SemanticsNodeInteractionsProvider.reposField
        get() = onNodeWithTag("repos-field")

    private val SemanticsNodeInteractionsProvider.allReposSwitch
        get() = onNodeWithTag("all-repos-switch")

    private val SemanticsNodeInteractionsProvider.addButtonInDialog
        get() = onNodeWithTag("add-button-in-dialog")

    @Test
    internal fun `have settings retained after the user logs in again`() =
        runPinghUiTest {
            toSettingsPage()
            awaitFact { dndOption.assertExists() }
            dndOption.performClick()
            awaitFact { dndOption.assertIsOn() }
            logoutButton.performClick()
            awaitFact { logoutButton.assertDoesNotExist() }
            toSettingsPage()
            awaitFact { dndOption.assertIsOn() }
        }

    @Test
    internal fun `store settings separately for each user`() =
        runPinghUiTest {
            toSettingsPage()
            snoozeTimeOption.onChildAt(0).performClick()
            awaitFact { snoozeTimeOption.onChildAt(0).assertIsSelected() }
            logoutButton.performClick()
            awaitFact { logoutButton.assertDoesNotExist() }
            toSettingsPage("User")
            awaitFact { snoozeTimeOption.onChildAt(0).assertIsNotSelected() }
            snoozeTimeOption.onChildAt(2).performClick()
            awaitFact { snoozeTimeOption.onChildAt(2).assertIsSelected() }
            logoutButton.performClick()
            awaitFact { logoutButton.assertDoesNotExist() }
            toSettingsPage()
            awaitFact { snoozeTimeOption.onChildAt(0).assertIsSelected() }
        }

    @Nested internal inner class
    `Have opened dialog for adding a new ignores source, and` {

        @Test
        internal fun `have 'Add' button disabled if organization name is entered incorrectly`() =
            runPinghUiTest {
                openDialog()
                orgField.performTextInput("illegal@name")
                awaitFact { addButtonInDialog.assertIsNotEnabled() }
            }

        @ParameterizedTest
        @ValueSource(strings = ["illegal@name", "wrong|separator", "trailing, comma,"])
        internal fun `have 'Add' button disable if repositories names are entered incorrectly`(
            repos: String
        ) = runPinghUiTest {
            openDialog()
            orgField.performTextInput("spine-examples")
            reposField.performTextInput(repos)
            awaitFact { addButtonInDialog.assertIsNotEnabled() }
        }

        @Test
        internal fun `have 'Add' button enabled if org and repo names entered correctly`() =
            runPinghUiTest {
                openDialog()
                orgField.performTextInput("spine-examples")
                reposField.performTextInput("Pingh")
                awaitFact { addButtonInDialog.assertIsEnabled() }
            }

        @Test
        internal fun `have 'Add' button enabled if org name is correct and 'All Repos' selected`() =
            runPinghUiTest {
                openDialog()
                orgField.performTextInput("spine-examples")
                reposField.performTextInput("illegal@name")
                allReposSwitch.performClick()
                awaitFact { addButtonInDialog.assertIsEnabled() }
            }

        private fun ComposeUiTest.openDialog() {
            toSettingsPage()
            addButton.performClick()
            awaitFact { orgField.assertExists() }
        }
    }

    @Test
    internal fun `have ignored source in the list when it was added`() =
        runPinghUiTest {
            toSettingsPage()
            awaitFact {
                ignoredSources.onChildren().fetchSemanticsNodes().shouldBeEmpty()
            }
            addToIgnored("spine-examples", "Pingh")
            awaitFact {
                ignoredSources.onChildren().fetchSemanticsNodes() shouldHaveSize 1
                ignoredSources.onChildAt(0).assertTextContains("spine-examples/Pingh")
            }
        }

    @Test
    internal fun `have several ignored sources in list when repos were added by one action`() =
        runPinghUiTest {
            val repos = listOf("Pingh", "HelloWorld", "CoolProject")
            toSettingsPage()
            awaitFact {
                ignoredSources.onChildren().fetchSemanticsNodes().shouldBeEmpty()
            }
            addToIgnored("spine-examples", repos.joinToString(", "))
            awaitFact {
                ignoredSources.onChildren().fetchSemanticsNodes() shouldHaveSize repos.size
                repos.sorted().forEachIndexed { index, repo ->
                    ignoredSources.onChildAt(index).assertTextContains("spine-examples/$repo")
                }
            }
        }

    @Test
    internal fun `have several ignored sources in list when repos were added by several actions`() =
        runPinghUiTest {
            val repos = listOf("Pingh", "HelloWorld", "CoolProject")
            toSettingsPage()
            awaitFact {
                ignoredSources.onChildren().fetchSemanticsNodes().shouldBeEmpty()
            }
            repos.forEach { repo ->
                addToIgnored("spine-examples", repo)
            }
            awaitFact {
                ignoredSources.onChildren().fetchSemanticsNodes() shouldHaveSize repos.size
                repos.sorted().forEachIndexed { index, repo ->
                    ignoredSources.onChildAt(index).assertTextContains("spine-examples/$repo")
                }
            }
        }

    @Test
    internal fun `have 'Remove' button disable if no ignore sources are selected`() =
        runPinghUiTest {
            toSettingsPage()
            addToIgnored("spine-examples", "Pingh")
            awaitFact {
                ignoredSources.onChildren().fetchSemanticsNodes() shouldHaveSize 1
                removeButton.assertIsNotEnabled()
            }
        }

    @Test
    internal fun `have 'Remove' button enables if any ignore source is selected`() =
        runPinghUiTest {
            toSettingsPage()
            addToIgnored("spine-examples", "Pingh")
            awaitFact {
                ignoredSources.onChildren().fetchSemanticsNodes() shouldHaveSize 1
            }
            ignoredSources.onChildAt(0).performClick()
            awaitFact { removeButton.assertIsEnabled() }
        }

    @Test
    internal fun `have empty list when all ignored sources were removed`() =
        runPinghUiTest {
            toSettingsPage()
            awaitFact {
                ignoredSources.onChildren().fetchSemanticsNodes().shouldBeEmpty()
            }
            addToIgnored("spine-examples", "Pingh")
            awaitFact {
                ignoredSources.onChildren().fetchSemanticsNodes() shouldHaveSize 1
            }
            ignoredSources.onChildAt(0).performClick()
            awaitFact { removeButton.assertIsEnabled() }
            removeButton.performClick()
            awaitFact {
                ignoredSources.onChildren().fetchSemanticsNodes().shouldBeEmpty()
            }
        }

    @Test
    internal fun `have ignored repository removed if its organization is added to the list`() =
        runPinghUiTest {
            toSettingsPage()
            addToIgnored("spine-examples", "Pingh")
            awaitFact {
                ignoredSources.onChildren().fetchSemanticsNodes() shouldHaveSize 1
            }
            addToIgnored("spine-examples")
            awaitFact {
                ignoredSources.onChildren().fetchSemanticsNodes() shouldHaveSize 1
                ignoredSources.onChildAt(0).assertTextContains("spine-examples [All repos]")
            }
        }

    private fun ComposeUiTest.toSettingsPage(user: String = username) {
        logIn(user)
        awaitFact { menuButton.assertExists() }
        menuButton.performClick()
        awaitFact { settingsButton.assertExists() }
        settingsButton.performClick()
        awaitFact { logoutButton.assertExists() }
    }

    /**
     * Adds specified repositories to the list of ignored sources.
     * If no repositories are specified, all repositories within the given organization
     * are added to the ignored list.
     */
    private fun ComposeUiTest.addToIgnored(org: String, repos: String? = null) {
        addButton.performClick()
        awaitFact { orgField.assertExists() }
        orgField.performTextInput(org)
        if (repos == null) {
            allReposSwitch.performClick()
        } else {
            reposField.performTextInput(repos)
        }
        awaitFact { addButtonInDialog.assertIsEnabled() }
        addButtonInDialog.performClick()
        awaitFact { addButtonInDialog.assertDoesNotExist() }
    }
}
