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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.google.protobuf.util.Timestamps
import io.spine.examples.pingh.client.DesktopClient
import io.spine.examples.pingh.mentions.MentionId
import io.spine.examples.pingh.mentions.MentionStatus
import io.spine.examples.pingh.mentions.MentionView
import java.lang.Thread.sleep

/**
 * Maximum length for the title of the mention card.
 *
 * If the title exceeds this length, it will be trimmed to fit the card.
 */
private const val maxLengthOfMentionCardTitle = 27

/**
 * Order of statuses for sorting mentions in the [sortByStatusAndWhenMentioned] method.
 */
private val statusOrder = mapOf(
    MentionStatus.UNREAD to 0,
    MentionStatus.SNOOZED to 1,
    MentionStatus.READ to 2
)

/**
 * Displays the 'Home' page in the application.
 *
 * This page is the main interface where users can manage their mentions.
 * Users can snooze and read mentions on this page. Additionally, it is
 * possible to manually update the list of mentions from the server.
 */
@Composable
internal fun HomePage(client: DesktopClient) {
    val model = remember { HomePageState(client) }
    Column(
        Modifier.fillMaxSize()
    ) {
        ToolBar(model)
        MentionCards(model)
    }
}

/**
 * Displays a menu of tools for navigating to a user profile page or
 * manually updating mentions.
 */
@Composable
private fun ToolBar(model: HomePageState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(MaterialTheme.colorScheme.primary)
            .drawBehind {
                drawLine(
                    color = Color.Black,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            icon = Icons.profile,
            onClick = { }, // Go to the `Profile` page.
            modifier = Modifier.size(40.dp)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = "Pingh",
            modifier = Modifier.width(120.dp),
            style = MaterialTheme.typography.displayLarge
        )
        Spacer(Modifier.width(5.dp))
        IconButton(
            icon = Icons.refresh,
            onClick = {
                model.updateMentions()
            },
            modifier = Modifier.size(40.dp)
        )
    }
}

/**
 * Displays all mentions of the user, with unread mentions listed first,
 * followed by snoozed mentions, and finally read mentions.
 *
 * Within each group, mentions are sorted by time.
 */
@Composable
private fun MentionCards(model: HomePageState) {
    val scrollState = rememberScrollState()
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .verticalScroll(scrollState)
            .background(MaterialTheme.colorScheme.background),
    ) {
        model.mentions()
            .sortByStatusAndWhenMentioned()
            .forEach { mention ->
                Spacer(Modifier.height(20.dp))
                MentionCard(model, mention)
            }
        Spacer(Modifier.height(20.dp))
    }
}

/**
 * Displays all information about a particular mention.
 *
 * Depending on the status of the mention, the card design and possible interactions vary.
 *
 * - If the mention is unread, it can be read or snoozed.
 *
 * - If the mention is snoozed, it can only be read.
 *
 * - If the mention is read, it can still be opened, but its status does not change.
 */
@Composable
private fun MentionCard(model: HomePageState, mention: MentionView) {
    val uriHandler = LocalUriHandler.current
    val mentionIsRead = mention.status == MentionStatus.READ
    val containerColor = if (mentionIsRead) {
        MaterialTheme.colorScheme.onBackground
    } else {
        MaterialTheme.colorScheme.primary
    }
    val onClick = {
        uriHandler.openUri(mention.url.spec)
        if (!mentionIsRead) {
            model.markMentionAsRead(mention.id)
        }
    }
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        colors = CardDefaults.elevatedCardColors().copy(
            containerColor = containerColor
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
            MentionCardText(mention)
            Spacer(Modifier.width(5.dp))
            SnoozeButton(model, mention)
        }
    }
}

/**
 * Displays textual information about the mention,
 * including details about who mentioned the user, where, and when.
 */
@Composable
private fun MentionCardText(mention: MentionView) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(90.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "${mention.whoMentioned.username.value}/${mention.title}"
                .truncate(maxLengthOfMentionCardTitle, "..."),
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = mention.whenMentioned.toDatetime(),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * Displays the snooze button if the mention is unread. If the mention is snoozed,
 * it displays the status text of the mention.
 *
 * Otherwise, nothing is displayed.
 */
@Composable
private fun SnoozeButton(model: HomePageState, mention: MentionView) {
    when (mention.status) {
        MentionStatus.UNREAD ->
            IconButton(
                icon = Icons.snooze,
                onClick = {
                    model.markMentionAsSnoozed(mention.id)
                },
                modifier = Modifier.size(40.dp)
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
 * State of [HomePage].
 *
 * This state is a layer between `@Composable` function and `DesktopClient`.
 */
private class HomePageState(private val client: DesktopClient) {

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
     */
    internal fun markMentionAsSnoozed(id: MentionId) {
        client.markMentionAsSnoozed(id) {
            mentions.value = mentions.value.setMentionStatus(id, MentionStatus.SNOOZED)
        }
    }

    /**
     * Marks that the mention is read.
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
 * Creates a new list by replacing the status of one mention with another.
 */
private fun MentionsList.setMentionStatus(id: MentionId, status: MentionStatus): MentionsList {
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
 *
 * @see [statusOrder]
 */
private fun MentionsList.sortByStatusAndWhenMentioned(): MentionsList =
    this.sortedWith { firstMentions, secondMentions ->
        val statusComparison = statusOrder[firstMentions.status]!!
            .compareTo(statusOrder[secondMentions.status]!!)
        if (statusComparison != 0) {
            statusComparison
        } else {
            Timestamps.compare(secondMentions.whenMentioned, firstMentions.whenMentioned)
        }
    }
