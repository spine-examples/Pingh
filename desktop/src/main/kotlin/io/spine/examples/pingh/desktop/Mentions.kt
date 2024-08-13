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

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.protobuf.util.Timestamps
import io.spine.examples.pingh.client.DesktopClient
import io.spine.examples.pingh.mentions.MentionId
import io.spine.examples.pingh.mentions.MentionStatus
import io.spine.examples.pingh.mentions.MentionView
import java.lang.Thread.sleep

/**
 * Displays the `Mentions` page in the application.
 *
 * This page is the main interface where users can manage their mentions.
 * Users can snooze and read mentions on this page. Additionally, it is
 * possible to manually update the list of mentions from the server.
 *
 * @param client enables interaction with the Pingh server.
 * @param settings the state of the application settings.
 * @param toSettingsPage the navigation to the 'Settings' page.
 */
@Composable
internal fun MentionsPage(
    client: DesktopClient,
    settings: SettingsState,
    toSettingsPage: () -> Unit
) {
    val state = remember { MentionsPageState(client) }
    Column(
        Modifier.fillMaxSize()
    ) {
        ToolBar(state, toSettingsPage)
        Spacer(Modifier.height(0.5.dp))
        MentionCards(state, settings.snoozeTime.value)
    }
}

/**
 * Displays a menu of tools for navigating to an application settings page or
 * manually updating mentions.
 *
 * @param state the state of the `MentionsPage`.
 * @param toSettingsPage the navigation to the 'Settings' page.
 */
@Composable
private fun ToolBar(
    state: MentionsPageState,
    toSettingsPage: () -> Unit
) {
    val contentColor = MaterialTheme.colorScheme.onSecondary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(MaterialTheme.colorScheme.secondary)
            .drawBehind {
                drawLine(
                    color = contentColor,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(horizontal = 5.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            icon = Icons.pingh,
            onClick = toSettingsPage,
            modifier = Modifier.size(40.dp),
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = contentColor
            )
        )
        Text(
            text = "Recent mentions",
            modifier = Modifier.width(140.dp),
            color = contentColor,
            style = MaterialTheme.typography.displayLarge
        )
        IconButton(
            icon = Icons.refresh,
            onClick = {
                state.updateMentions()
            },
            modifier = Modifier.size(40.dp),
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = contentColor
            )
        )
    }
}

/**
 * Displays all mentions of the user, with unread mentions listed first,
 * followed by snoozed mentions, and finally read mentions.
 *
 * Within each group, mentions are sorted by time.
 *
 * @param state the state of the `MentionsPage`.
 * @param snoozeTime the interval after which the notification is repeated.
 */
@Composable
private fun MentionCards(
    state: MentionsPageState,
    snoozeTime: SnoozeTime
) {
    val scrollState = rememberScrollState()
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 5.dp)
            .verticalScroll(scrollState)
            .background(MaterialTheme.colorScheme.background),
    ) {
        state.mentions()
            .sortByStatusAndWhenMentioned()
            .forEach { mention ->
                Spacer(Modifier.height(5.dp))
                MentionCard(state, mention, snoozeTime)
            }
        Spacer(Modifier.height(5.dp))
    }
}

/**
 * Displays all information about a particular mention.
 *
 * Depending on the status of the mention, the card design and possible interactions vary.
 *
 * - If the mention is unread, it can be read or snoozed.
 * - If the mention is snoozed, it can only be read.
 * - If the mention is read, it can still be opened, but its status does not change.
 *
 * @param state the state of the `MentionsPage`.
 * @param mention the mention whose information is displayed.
 * @param snoozeTime the interval after which the notification is repeated.
 */
@Composable
private fun MentionCard(
    state: MentionsPageState,
    mention: MentionView,
    snoozeTime: SnoozeTime
) {
    val uriHandler = LocalUriHandler.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered = interactionSource.collectIsHoveredAsState()
    val mentionIsRead = mention.status == MentionStatus.READ
    val containerColor = if (mentionIsRead) {
        MaterialTheme.colorScheme.onBackground
    } else {
        MaterialTheme.colorScheme.secondary
    }
    val onClick = {
        uriHandler.openUri(mention.url.spec)
        if (!mentionIsRead) {
            state.markMentionAsRead(mention.id)
        }
    }
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        interactionSource = interactionSource,
        colors = CardDefaults.elevatedCardColors(
            containerColor = containerColor,
            contentColor = MaterialTheme.colorScheme.onSecondary
        )
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 3.dp, horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(
                url = mention.whoMentioned.avatarUrl,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.width(5.dp))
            MentionCardText(mention, isHovered)
            Spacer(Modifier.width(5.dp))
            SnoozeButton(state, mention, snoozeTime)
        }
    }
}

/**
 * Displays textual information about the mention,
 * including details about who mentioned the user, where, and when.
 *
 * @param mention the mention whose information is displayed.
 * @param isHovered indicates whether the mouse is over the mention card.
 */
@Composable
private fun MentionCardText(
    mention: MentionView,
    isHovered: State<Boolean>
) {
    val time = mention.whenMentioned.run {
        if (isHovered.value) toDatetime() else howMuchTimeHasPassed()
    }
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(120.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = mention.title,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "$time, by ${mention.whoMentioned.username.value}",
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

/**
 * Displays the snooze button if the mention is unread. If the mention is snoozed,
 * it displays the status text of the mention.
 *
 * Otherwise, nothing is displayed.
 *
 * @param state the state of the `MentionsPage`.
 * @param mention the mention whose information is displayed.
 * @param snoozeTime the interval after which the notification is repeated.
 */
@Composable
private fun SnoozeButton(
    state: MentionsPageState,
    mention: MentionView,
    snoozeTime: SnoozeTime
) {
    when (mention.status) {
        MentionStatus.UNREAD ->
            IconButton(
                icon = Icons.snooze,
                onClick = {
                    state.markMentionAsSnoozed(mention.id, snoozeTime)
                },
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            )

        MentionStatus.SNOOZED ->
            Text(
                text = "Snoozed",
                modifier = Modifier.size(40.dp)
                    .wrapContentSize(Alignment.Center),
                style = MaterialTheme.typography.bodySmall
            )

        else -> {}
    }
}

/**
 * State of [MentionsPage].
 *
 * @param client enables interaction with the Pingh server.
 */
private class MentionsPageState(private val client: DesktopClient) {

    private companion object {
        /**
         * Delay before reading mentions so that the read-side on the server can be updated.
         *
         * Time is specified in milliseconds.
         */
        private const val delayBeforeReadingMentions = 100L
    }

    /**
     * User mentions.
     */
    private var mentions: MutableState<MentionsList> = mutableStateOf(client.findUserMentions())

    /**
     * Returns all user mentions.
     */
    internal fun mentions(): MentionsList = mentions.value

    /**
     * Updates the user mentions.
     */
    internal fun updateMentions() {
        client.updateMentions(
            onSuccess = {
                sleep(delayBeforeReadingMentions)
                mentions.value = client.findUserMentions()
            }
        )
    }

    /**
     * Marks the mention as snoozed.
     *
     * @param id the identifier of the mention that is marked as snoozed.
     * @param snoozeTime the interval after which the notification is repeated.
     */
    internal fun markMentionAsSnoozed(id: MentionId, snoozeTime: SnoozeTime) {
        client.markMentionAsSnoozed(id, snoozeTime.value) {
            mentions.value = mentions.value.setMentionStatus(id, MentionStatus.SNOOZED)
        }
    }

    /**
     * Marks that the mention is read.
     *
     * @param id the identifier of the mention that is marked as read.
     */
    internal fun markMentionAsRead(id: MentionId) {
        client.markMentionAsRead(id) {
            mentions.value = mentions.value.setMentionStatus(id, MentionStatus.READ)
        }
    }
}

/**
 * List of `MentionsView`s.
 */
private typealias MentionsList = List<MentionView>

/**
 * Updates the status of the mention with the specified identifier to the new status.
 *
 * @param id the identifier of the mention which the status was changed.
 * @param status new status of the mention.
 */
private fun MentionsList.setMentionStatus(
    id: MentionId,
    status: MentionStatus
): MentionsList {
    val idInList = this.indexOfFirst { it.id == id }
    val updatedMention = this[idInList]
        .toBuilder()
        .setStatus(status)
        .vBuild()
    val newMentionsList = this.toMutableList()
    newMentionsList[idInList] = updatedMention
    return newMentionsList
}

/**
 * Returns a `MentionsList` sorted such that unread mentions come first,
 * followed by snoozed mentions, and read mentions last.
 *
 * Within each group, mentions are sorted by the time they were made.
 */
private fun MentionsList.sortByStatusAndWhenMentioned(): MentionsList =
    this.sortedWith { firstMentions, secondMentions ->
        val statusComparison = firstMentions.status.compareTo(secondMentions.status)
        if (statusComparison != 0) {
            statusComparison
        } else {
            Timestamps.compare(secondMentions.whenMentioned, firstMentions.whenMentioned)
        }
    }
