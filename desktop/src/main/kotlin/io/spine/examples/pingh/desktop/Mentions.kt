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
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.spine.example.pingh.desktop.generated.resources.Res
import io.spine.example.pingh.desktop.generated.resources.pingh
import io.spine.example.pingh.desktop.generated.resources.refresh
import io.spine.example.pingh.desktop.generated.resources.snooze
import io.spine.examples.pingh.client.MentionsFlow
import io.spine.examples.pingh.client.howMuchTimeHasPassed
import io.spine.examples.pingh.client.sorted
import io.spine.examples.pingh.mentions.MentionStatus
import io.spine.examples.pingh.mentions.MentionView
import org.jetbrains.compose.resources.painterResource

/**
 * Displays the `Mentions` page in the application.
 *
 * This page is the main interface where users can manage their mentions.
 * Users can snooze and read mentions on this page. Additionally, it is
 * possible to manually update the list of mentions from the server.
 *
 * @param flow The flow for managing the lifecycle of mentions.
 * @param toSettingsPage The navigation to the 'Settings' page.
 */
@Composable
internal fun MentionsPage(
    flow: MentionsFlow,
    toSettingsPage: () -> Unit
) {
    Column(
        Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ToolBar(flow, toSettingsPage)
        Spacer(Modifier.height(0.5.dp))
        MentionCards(flow)
    }
}

/**
 * Displays a menu of tools for navigating to an application settings page or
 * manually updating mentions.
 *
 * @param flow The flow for managing the lifecycle of mentions.
 * @param toSettingsPage The navigation to the 'Settings' page.
 */
@Composable
private fun ToolBar(
    flow: MentionsFlow,
    toSettingsPage: () -> Unit
) {
    val contentColor = MaterialTheme.colorScheme.onSecondary
    val borderColor = MaterialTheme.colorScheme.onBackground
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(MaterialTheme.colorScheme.secondary)
            .drawBehind {
                drawLine(
                    color = borderColor,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(start = 27.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            icon = painterResource(Res.drawable.pingh),
            onClick = toSettingsPage,
            modifier = Modifier.size(56.dp).testTag("settings-button"),
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = contentColor
            )
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = "Recent mentions",
            modifier = Modifier.width(250.dp),
            fontSize = 20.sp,
            color = contentColor,
            style = MaterialTheme.typography.displayLarge
        )
        IconButton(
            icon = painterResource(Res.drawable.refresh),
            onClick = {
                flow.updateMentions()
            },
            modifier = Modifier.size(50.dp),
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
 * @param flow The flow for managing the lifecycle of mentions.
 */
@Composable
private fun MentionCards(
    flow: MentionsFlow
) {
    val mentions by flow.mentions.collectAsState()
    val scrollState = rememberScrollState()
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp)
            .verticalScroll(scrollState)
            .background(MaterialTheme.colorScheme.background)
            .testTag("mention-cards"),
    ) {
        mentions.sorted()
            .forEach { mention ->
                Spacer(Modifier.height(10.dp))
                MentionCard(flow, mention)
            }
        Spacer(Modifier.height(10.dp))
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
 * @param flow The flow for managing the lifecycle of mentions.
 * @param mention The mention whose information is displayed.
 */
@Composable
private fun MentionCard(
    flow: MentionsFlow,
    mention: MentionView
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
            flow.markAsRead(mention.id)
        }
    }
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mention-card-${mention.id}")
            .height(60.dp),
        interactionSource = interactionSource,
        colors = CardDefaults.elevatedCardColors(
            containerColor = containerColor,
            contentColor = MaterialTheme.colorScheme.onSecondary
        )
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(
                url = mention.whoMentioned.avatarUrl,
                modifier = Modifier.size(50.dp)
            )
            Spacer(Modifier.width(10.dp))
            MentionCardText(mention, isHovered)
            Spacer(Modifier.width(10.dp))
            SnoozeButton(flow, mention, isHovered.value)
        }
    }
}

/**
 * Displays textual information about the mention,
 * including details about who mentioned the user, where, and when.
 *
 * @param mention The mention whose information is displayed.
 * @param isHovered Indicates whether the mouse is over the mention card.
 */
@Composable
private fun MentionCardText(
    mention: MentionView,
    isHovered: State<Boolean>
) {
    val time = mention.whenMentioned.run {
        if (isHovered.value) toDatetime() else howMuchTimeHasPassed()
    }
    val textWidth = if (isHovered.value || mention.status == MentionStatus.SNOOZED) {
        240.dp
    } else {
        300.dp
    }
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(textWidth),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = mention.title,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(4.dp))
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
 * @param flow The flow for managing the lifecycle of mentions.
 * @param mention The mention whose information is displayed.
 * @param isParentHovered Whether the parent mention card is being hovered.
 */
@Composable
private fun SnoozeButton(
    flow: MentionsFlow,
    mention: MentionView,
    isParentHovered: Boolean
) {
    when {
        isParentHovered && mention.status == MentionStatus.UNREAD ->
            IconButton(
                icon = painterResource(Res.drawable.snooze),
                onClick = {
                    flow.snooze(mention.id)
                },
                modifier = Modifier.size(50.dp).testTag("snooze-button"),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            )

        mention.status == MentionStatus.SNOOZED ->
            Text(
                text = "Snoozed",
                modifier = Modifier.size(50.dp)
                    .wrapContentSize(Alignment.Center),
                style = MaterialTheme.typography.bodySmall
            )
    }
}
