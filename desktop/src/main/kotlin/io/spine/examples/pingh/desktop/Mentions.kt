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

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.spine.example.pingh.desktop.generated.resources.Res
import io.spine.example.pingh.desktop.generated.resources.pin
import io.spine.example.pingh.desktop.generated.resources.pingh
import io.spine.example.pingh.desktop.generated.resources.pinned
import io.spine.example.pingh.desktop.generated.resources.refresh
import io.spine.example.pingh.desktop.generated.resources.snooze
import io.spine.examples.pingh.client.MentionsFlow
import io.spine.examples.pingh.client.howMuchTimeHasPassed
import io.spine.examples.pingh.client.sorted
import io.spine.examples.pingh.mentions.MentionStatus
import io.spine.examples.pingh.mentions.MentionView
import java.awt.Cursor
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
 * @param appIconMultiplier The proportion of the button's size that the app icon occupies.
 */
@Composable
private fun ToolBar(
    flow: MentionsFlow,
    toSettingsPage: () -> Unit,
    appIconMultiplier: Float = 0.8f
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
            .padding(start = 20.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            icon = painterResource(Res.drawable.pingh),
            onClick = toSettingsPage,
            modifier = Modifier.size(50.dp).testTag("settings-button"),
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = contentColor
            ),
            sizeMultiplier = appIconMultiplier
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = "Recent mentions",
            modifier = Modifier.weight(1f),
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
    val list = remember(mentions) { mentions.sorted() }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
            .background(MaterialTheme.colorScheme.background)
            .testTag("mention-cards"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(list.size, key = { index -> list[index].id }) { index ->
            MentionCard(flow, list[index])
        }
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
private fun LazyItemScope.MentionCard(
    flow: MentionsFlow,
    mention: MentionView
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val mentionIsRead = mention.status == MentionStatus.READ
    val containerColor = if (mentionIsRead) {
        MaterialTheme.colorScheme.onBackground
    } else {
        MaterialTheme.colorScheme.secondary
    }
    val shape = MaterialTheme.shapes.medium
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mention-card-${mention.id}")
            .height(58.dp)
            .shadow(
                elevation = if (isHovered) 10.dp else 1.dp,
                shape = shape
            )
            .hoverable(interactionSource)
            .animateItem(
                placementSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                    visibilityThreshold = IntOffset.VisibilityThreshold
                )
            ),
        shape = shape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = containerColor,
            contentColor = MaterialTheme.colorScheme.onSecondary
        )
    ) {
        Row(
            modifier = Modifier.padding(start = 15.dp, end = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(
                url = mention.whoMentioned.avatarUrl,
                modifier = Modifier.size(40.dp)
            )
            MentionCardText(flow, mention, isHovered)
            Row(
                modifier = Modifier.fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SnoozeButton(flow, mention, isHovered)
                PinButton(flow, mention, isHovered)
            }
        }
    }
}

/**
 * Displays textual information about the mention,
 * including details about who mentioned the user, where, and when.
 *
 * @param flow The flow for managing the lifecycle of mentions.
 * @param mention The mention whose information is displayed.
 * @param isParentHovered Indicates whether the mouse is over the mention card.
 */
@Composable
private fun RowScope.MentionCardText(
    flow: MentionsFlow,
    mention: MentionView,
    isParentHovered: Boolean
) {
    val time = mention.whenMentioned.run {
        if (isParentHovered) toDatetime() else howMuchTimeHasPassed()
    }
    Column(
        modifier = Modifier.fillMaxHeight().weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
    ) {
        MentionTitle(flow, mention, isParentHovered)
        Text(
            text = "$time, by ${mention.whoMentioned.username.value}",
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

/**
 * Displays a mention header.
 *
 * Clicking the title navigates to the web page where user was mentioned.
 *
 * @param flow The flow for managing the lifecycle of mentions.
 * @param mention The mention whose information is displayed.
 * @param isParentHovered Indicates whether the mouse is over the mention card.
 */
@Composable
private fun MentionTitle(
    flow: MentionsFlow,
    mention: MentionView,
    isParentHovered: Boolean
) {
    val uriHandler = LocalUriHandler.current
    val readMention = {
        uriHandler.openUri(mention.url.spec)
        if (mention.status != MentionStatus.READ) {
            flow.markAsRead(mention.id)
        }
    }
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val color = if (isHovered) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSecondary
    }
    Text(
        text = mention.title,
        modifier = Modifier
            .run {
                if (isParentHovered) {
                    drawBehind {
                        val strokeWidthPx = 1.dp.toPx()
                        val verticalOffset = size.height - 1.sp.toPx()
                        drawLine(
                            color = color,
                            strokeWidth = strokeWidthPx,
                            start = Offset(0f, verticalOffset),
                            end = Offset(size.width, verticalOffset)
                        )
                    }
                } else {
                    this
                }
            }
            .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)))
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = readMention
            )
            .testTag("mention-title"),
        color = color,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        style = MaterialTheme.typography.bodyLarge
    )
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
            MentionIconButton(
                icon = painterResource(Res.drawable.snooze),
                onClick = { flow.snooze(mention.id) },
                modifier = Modifier.testTag("snooze-button")
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

/**
 * Displays the pin icon for a mention when it is not pinned,
 * and the mention card is hovered. If the mention is pinned,
 * the unpin icon is displayed instead.
 *
 * @param flow The flow for managing the lifecycle of mentions.
 * @param mention The mention whose information is displayed.
 * @param isParentHovered Whether the parent mention card is being hovered.
 * @param sizeMultiplier The proportion of the button's size that the icon occupies.
 */
@Composable
private fun PinButton(
    flow: MentionsFlow,
    mention: MentionView,
    isParentHovered: Boolean,
    sizeMultiplier: Float = 0.6f
) {
    val (iconRes, onClick) = when {
        isParentHovered && !mention.pinned -> Res.drawable.pin to { flow.pin(mention.id) }
        mention.pinned -> Res.drawable.pinned to { flow.unpin(mention.id) }
        else -> null to null
    }
    if (iconRes != null && onClick != null) {
        MentionIconButton(
            icon = painterResource(iconRes),
            onClick = onClick,
            modifier = Modifier.testTag("pin-button"),
            sizeMultiplier = sizeMultiplier
        )
    }
}

/**
 * Displays the icon button on the mention card.
 *
 * @param icon The painter to draw icon.
 * @param onClick Called when this icon button is clicked.
 * @param modifier The modifier to be applied to this icon button.
 * @param sizeMultiplier The proportion of the button's size that the icon occupies.
 */
@Composable
private fun MentionIconButton(
    icon: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sizeMultiplier: Float = 0.85f
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val color = if (isHovered) {
        MaterialTheme.colorScheme.onSecondary
    } else {
        MaterialTheme.colorScheme.surface
    }
    IconButton(
        icon = icon,
        onClick = onClick,
        modifier = Modifier.size(30.dp)
            .clip(CircleShape)
            .hoverable(interactionSource)
            .then(modifier),
        shape = CircleShape,
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = color
        ),
        sizeMultiplier = sizeMultiplier
    )
}
