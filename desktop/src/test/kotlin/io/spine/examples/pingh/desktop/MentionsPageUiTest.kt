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
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.spine.examples.pingh.desktop.given.DelayedFactAssertion.Companion.awaitFact
import io.spine.examples.pingh.desktop.given.testTag
import kotlin.test.Test
import org.junit.jupiter.api.DisplayName

@DisplayName("Mentions page should")
@OptIn(ExperimentalTestApi::class)
internal class MentionsPageUiTest : UiTest() {

    private val isSnoozeButton = hasTestTag("snooze-button")

    @Test
    internal fun `allow users to open a mentions URL even after it has been read`() =
        runComposeUiTest {
            runApp()
            logIn()
            val testTag = mentionCards().random().testTag
            onNodeWithTag(testTag).performClick()
            awaitFact { openedUrlCount() shouldBe 1 }
            onNodeWithTag(testTag).performClick()
            awaitFact { openedUrlCount() shouldBe 2 }
        }

    @Test
    internal fun `have snooze button disabled after it is clicked`() =
        runComposeUiTest {
            runApp()
            logIn()
            val testTag = mentionCards().random().testTag
            val snoozeButton = onNodeWithTag(testTag)
                .onChildren()
                .filterToOne(isSnoozeButton)
            snoozeButton.performClick()
            awaitFact { snoozeButton.assertDoesNotExist() }
        }

    @Test
    internal fun `have snooze button disabled if mention has been read`() =
        runComposeUiTest {
            runApp()
            logIn()
            val testTag = mentionCards().random().testTag
            val snoozeButton = onNodeWithTag(testTag)
                .onChildren()
                .filterToOne(isSnoozeButton)
            onNodeWithTag(testTag).performClick()
            awaitFact { snoozeButton.assertDoesNotExist() }
        }

    @Test
    internal fun `have mentions sorted after their states have been changed`() =
        runComposeUiTest {
            runApp()
            logIn()
            val mentionsCards = mentionCards().sortedBy { it.positionInRoot.y }
            val readMentionsTag = mentionsCards[0].testTag
            val snoozedMentionsTag = mentionsCards[1].testTag
            onNodeWithTag(readMentionsTag).performClick()
            onNodeWithTag(snoozedMentionsTag)
                .onChildren()
                .filterToOne(isSnoozeButton)
                .performClick()
            awaitFact {
                val mentions = mentionCards()
                val readMention = mentions.first { it.testTag == readMentionsTag }
                val snoozedMention = mentions.first { it.testTag == snoozedMentionsTag }
                snoozedMention.positionInRoot.y shouldBeLessThan readMention.positionInRoot.y
                mentions.forEach { mention ->
                    if (mention != readMention && mention != snoozedMention) {
                        mention.positionInRoot.y shouldBeLessThan readMention.positionInRoot.y
                        mention.positionInRoot.y shouldBeLessThan snoozedMention.positionInRoot.y
                    }
                }
            }
        }

    private fun SemanticsNodeInteractionsProvider.mentionCards(): List<SemanticsNode> =
        onNodeWithTag("mention-cards")
            .onChildren()
            .filter(hasClickAction())
            .fetchSemanticsNodes()
}
