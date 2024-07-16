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

package io.spine.examples.pingh.desktop.home

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import io.spine.examples.pingh.client.DesktopClient
import io.spine.examples.pingh.desktop.component.Avatar
import io.spine.examples.pingh.desktop.component.IconButton
import io.spine.examples.pingh.desktop.component.Icons
import io.spine.examples.pingh.mentions.MentionStatus
import io.spine.examples.pingh.mentions.MentionView

// Document.
@Composable
public fun HomePage(client: DesktopClient) {
    val model = remember { HomePageModel(client) }

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
private fun ToolBar(model: HomePageModel) {
    Row(
        Modifier
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
            .then(Modifier.padding(horizontal = 10.dp, vertical = 4.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            icon = Icons.profile,
            onClick = { }, // Go to the `Profile` page.
            modifierExtension = { this.size(40.dp) }
        )
        Spacer(Modifier.width(5.dp))
        Text(
            "Pingh",
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.width(120.dp)
        )
        Spacer(Modifier.width(5.dp))
        IconButton(
            icon = Icons.refresh,
            onClick = { model.updateMentions() },
            modifierExtension = { this.size(40.dp) }
        )
    }
}

/**
 * Displays all mentions of the user, with the unread mentions coming first,
 * then coming snoozed, and in the last read.
 *
 * Within each group, the mentions are sorted by time.
 */
@Composable
private fun MentionCards(model: HomePageModel) {
    // Sort mentions by status.
    val scrollState = rememberScrollState()
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .verticalScroll(scrollState)
            .background(MaterialTheme.colorScheme.background),
    ) {
        for (mention in model.mentions()) {
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
 * - If the mention is unread, it can be read or snooze.
 *
 * - If the mention is snoozed, it can only be read.
 *
 * - If the mention is read, it can still be opened, but its status isn't changed.
 */
@Composable
private fun MentionCard(model: HomePageModel, mention: MentionView) {
    val mentionIsRead = mention.status == MentionStatus.READ
    val containerColor: Color = if (mentionIsRead) {
        MaterialTheme.colorScheme.onBackground
    } else {
        MaterialTheme.colorScheme.primary
    }
    val onClick: () -> Unit = {
        LocalUriHandler.current
            .openUri(mention.url.spec)
        if (!mentionIsRead) {
            model.markMentionAsRead(mention.id)
        }
    }
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        colors = CardDefaults.elevatedCardColors().copy(
            containerColor = containerColor
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 5.dp, horizontal = 10.dp)
        ) {
            Avatar(
                url = mention.whoMentioned.avatarUrl,
                modifierExtender = { this.size(40.dp) }
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
 * namely who mentioned the user, where and when.
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
            "${mention.whoMentioned.username.value}/${mention.title}",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            mention.whenMentioned.toString(), // Create Timestamp extension for displaying time.
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * Displays the snooze button if the mention is not read, if the mention is snoozed it displays
 * the text with the status of the mention.
 *
 * In all other cases, nothing will be displayed.
 */
@Composable
private fun SnoozeButton(model: HomePageModel, mention: MentionView) {
    when (mention.status) {
        MentionStatus.UNREAD ->
            IconButton(
                icon = Icons.snooze,
                onClick = {
                    model.markMentionAsSnoozed(mention.id)
                },
                modifierExtension = { this.size(40.dp) }
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
