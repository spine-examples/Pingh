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

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.spine.examples.pingh.desktop.given.DelayedFactAssertion.Companion.awaitFact
import io.spine.examples.pingh.desktop.given.performHover
import io.spine.examples.pingh.desktop.given.testTag
import kotlin.test.Test
import org.junit.jupiter.api.DisplayName

@DisplayName("Mentions page should")
@OptIn(ExperimentalTestApi::class)
internal class MentionsPageUiTest : UiTest() {

    private val SemanticsNodeInteractionsProvider.menuButton
        get() = onNodeWithTag("menu-button")

    private val SemanticsNodeInteractionsProvider.markAllAsReadButton
        get() = onNodeWithTag("mark-all-as-read-button")

    @Test
    internal fun `allow users to open a mentions URL even after it has been read`() =
        runPinghUiTest {
            logIn()
            awaitFact { mentionCards().size shouldBeGreaterThanOrEqual 1 }
            val tag = mentionCards().random().testTag
            onTitleWithParentTag(tag).performClick()
            awaitFact { openedUrlCount() shouldBe 1 }
            onTitleWithParentTag(tag).performClick()
            awaitFact { openedUrlCount() shouldBe 2 }
        }

    @Test
    internal fun `have snooze button disabled after it is clicked`() =
        runPinghUiTest {
            logIn()
            awaitFact { mentionCards().size shouldBeGreaterThanOrEqual 1 }
            val tag = mentionCards().random().testTag
            clickSnoozeButton(tag)
            awaitFact {
                onNodeWithTag(tag).performHover()
                onSnoozeButtonWithParentTag(tag).assertDoesNotExist()
            }
        }

    @Test
    internal fun `have snooze button disabled if mention has been read`() =
        runPinghUiTest {
            logIn()
            awaitFact { mentionCards().size shouldBeGreaterThanOrEqual 1 }
            val tag = mentionCards().random().testTag
            onTitleWithParentTag(tag).performClick()
            awaitFact {
                onNodeWithTag(tag).performHover()
                onSnoozeButtonWithParentTag(tag).assertDoesNotExist()
            }
        }

    @Test
    internal fun `have mentions sorted after their states have been changed`() =
        runPinghUiTest {
            logIn()
            awaitFact { mentionCards().size shouldBeGreaterThanOrEqual 2 }
            val mentionsCards = mentionCards().sortedBy { it.positionInRoot.y }
            val readMentionTag = mentionsCards[0].testTag
            val snoozedMentionTag = mentionsCards[1].testTag
            clickSnoozeButton(snoozedMentionTag)
            onTitleWithParentTag(readMentionTag).performClick()
            awaitFact {
                val mentions = mentionCards()
                val readMention = mentions.first { it.testTag == readMentionTag }
                val snoozedMention = mentions.first { it.testTag == snoozedMentionTag }
                snoozedMention.positionInRoot.y shouldBeLessThan readMention.positionInRoot.y
                mentions.forEach { mention ->
                    if (mention != readMention && mention != snoozedMention) {
                        mention.positionInRoot.y shouldBeLessThan readMention.positionInRoot.y
                        mention.positionInRoot.y shouldBeLessThan snoozedMention.positionInRoot.y
                    }
                }
            }
        }

    @Test
    internal fun `have pinned mention displayed at the top of the mention list`() =
        runPinghUiTest {
            logIn()
            awaitFact { mentionCards().size shouldBeGreaterThanOrEqual 1 }
            val tag = mentionCards().random().testTag
            clickPinButton(tag)
            awaitFact {
                val mentions = mentionCards()
                val pinnedMention = mentions.first { it.testTag == tag }
                mentions.forEach { mention ->
                    if (mention != pinnedMention) {
                        mention.positionInRoot.y shouldBeGreaterThan pinnedMention.positionInRoot.y
                    }
                }
            }
        }

    @Test
    internal fun `have pin button displayed when mention is pinned and not displayed otherwise`() =
        runPinghUiTest {
            logIn()
            awaitFact { mentionCards().size shouldBeGreaterThanOrEqual 2 }
            val mentions = mentionCards().shuffled()
            val pinnedTag = mentions[0].testTag
            val anotherTag = mentions[1].testTag
            clickPinButton(pinnedTag)
            onNodeWithTag(anotherTag).performHover()
            awaitFact { onPinButtonWithParentTag(pinnedTag).assertExists() }
        }

    @Test
    internal fun `have unpinned mentions returned to their original position`() =
        runPinghUiTest {
            logIn()
            awaitFact { mentionCards().size shouldBeGreaterThanOrEqual 2 }
            val (tag, initPosition) = mentionCards()
                .sortedByDescending { it.positionInRoot.y }[0]
                .run { testTag to positionInRoot }
            clickPinButton(tag)
            awaitFact {
                onNodeWithTag(tag).fetchSemanticsNode()
                    .positionInRoot.y shouldBeLessThan initPosition.y
            }
            clickPinButton(tag)
            awaitFact {
                onNodeWithTag(tag).fetchSemanticsNode()
                    .positionInRoot.y shouldBe initPosition.y
            }
        }

    @Test
    internal fun `have pinned mentions sorted separately from unpinned ones`() =
        runPinghUiTest {
            logIn()
            awaitFact { mentionCards().size shouldBeGreaterThanOrEqual 3 }
            var mentions = mentionCards().sortedByDescending { it.positionInRoot.y }
            val pinnedTag = mentions[0].testTag
            val readPinnedTag = mentions[1].testTag
            clickPinButton(pinnedTag)
            clickPinButton(readPinnedTag)
            onTitleWithParentTag(readPinnedTag).performClick()
            awaitFact {
                mentions = mentionCards()
                val pinned = mentions.first { it.testTag == pinnedTag }
                val readPinned = mentions.first { it.testTag == readPinnedTag }
                readPinned.positionInRoot.y shouldBeGreaterThan pinned.positionInRoot.y
                mentions.forEach { mention ->
                    if (mention != pinned && mention != readPinned) {
                        mention.positionInRoot.y shouldBeGreaterThan readPinned.positionInRoot.y
                    }
                }
            }
        }

    @Test
    internal fun `have all mentions read when 'Mark all as read' button is clicked`() =
        runPinghUiTest {
            logIn()
            menuButton.performClick()
            awaitFact { markAllAsReadButton.assertExists() }
            markAllAsReadButton.performClick()
            awaitFact { unreadMentionCount() shouldBe 0 }
        }

    @Test
    internal fun `have not mentions from ignored sources displayed`() =
        runPinghUiTest {
            logIn()
            awaitFact { mentionCards().size shouldBeGreaterThanOrEqual 4 }
            val tags = mentionCards().map { it.testTag }
            awaitFact { menuButton.assertExists() }
            menuButton.performClick()
            awaitFact { onNodeWithTag("settings-button").assertExists() }
            onNodeWithTag("settings-button").performClick()
            awaitFact { onNodeWithTag("logout-button").assertExists() }
            onNodeWithTag("add-button").performClick()
            awaitFact { onNodeWithTag("org-field").assertExists() }
            onNodeWithTag("org-field").performTextInput("spine-examples")
            onNodeWithTag("repos-field").performTextInput("Pingh")
            awaitFact { onNodeWithTag("add-button-in-dialog").assertIsEnabled() }
            onNodeWithTag("add-button-in-dialog").performClick()
            awaitFact { onNodeWithTag("add-button-in-dialog").assertDoesNotExist() }
            onNodeWithTag("back-button").performClick()
            awaitFact {
                menuButton.assertExists()
                tags.forEach { tag ->
                    onNodeWithTag(tag).assertDoesNotExist()
                }
            }
        }

    private fun ComposeUiTest.clickSnoozeButton(tag: String) {
        onNodeWithTag(tag).performHover()
        awaitFact { onSnoozeButtonWithParentTag(tag).assertExists() }
        onSnoozeButtonWithParentTag(tag).performClick()
    }

    private fun ComposeUiTest.clickPinButton(tag: String) {
        onNodeWithTag(tag).performHover()
        awaitFact { onPinButtonWithParentTag(tag).assertExists() }
        onPinButtonWithParentTag(tag).performClick()
    }

    private fun SemanticsNodeInteractionsProvider.mentionCards(): List<SemanticsNode> =
        onNodeWithTag("mention-cards")
            .performScrollToIndex(0)
            .onChildren()
            .fetchSemanticsNodes()

    private fun SemanticsNodeInteractionsProvider.onTitleWithParentTag(tag: String):
            SemanticsNodeInteraction =
        onNode(hasTestTag("mention-title").and(hasAnyAncestor(hasTestTag(tag))))

    private fun SemanticsNodeInteractionsProvider.onSnoozeButtonWithParentTag(tag: String):
            SemanticsNodeInteraction =
        onNode(hasTestTag("snooze-button").and(hasAnyAncestor(hasTestTag(tag))))

    private fun SemanticsNodeInteractionsProvider.onPinButtonWithParentTag(tag: String):
            SemanticsNodeInteraction =
        onNode(hasTestTag("pin-button").and(hasAnyAncestor(hasTestTag(tag))))
}
