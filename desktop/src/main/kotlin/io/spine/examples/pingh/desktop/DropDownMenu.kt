/*
 * Copyright 2025, TeamDev. All rights reserved.
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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.PointerIcon.Companion.Hand
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow.Companion.Ellipsis
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import io.spine.example.pingh.desktop.generated.resources.Res
import io.spine.example.pingh.desktop.generated.resources.check
import io.spine.example.pingh.desktop.generated.resources.drop_down_arrow
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun <T> DropDownMenu(
    selected: T,
    onChangeValue: (T) -> Unit,
    width: Dp,
    modifier: Modifier = Modifier,
    items: List<DropDownMenuItem<T>>
) {
    check(items.count { it.value == selected } == 1) {
        "The selected item was missing from the menu list but must have been included. " +
                "Selected item: \"$selected\", all items: $items"
    }
    var shown by remember { mutableStateOf(false) }
    val selectedItem = remember(selected) { items.find { it.value == selected }!! }
    Box(modifier.width(width)) {
        if (shown) {
            Popup(
                popupPositionProvider = rememberPositionProvider(20.dp),
                onDismissRequest = { shown = false },
            ) {
                DropDownMenuContent(selectedItem, items, width + 20.dp) { value ->
                    shown = false
                    onChangeValue(value)
                }
            }
        } else {
            MenuLabel(selectedItem) { shown = true }
        }
    }
}

internal class DropDownMenuItem<T>(
    internal val text: String,
    internal val value: T
)

@Composable
private fun <T> MenuLabel(
    selectedItem: DropDownMenuItem<T>,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val border = BorderStroke(
        1.dp,
        MaterialTheme.colorScheme.run { if (hovered) secondaryContainer else secondary }
    )
    val iconColor = MaterialTheme.colorScheme
        .run { if (hovered) secondary else background }
    val iconBorder = BorderStroke(
        0.5.dp,
        MaterialTheme.colorScheme.run { if (hovered) secondary else onBackground }
    )
    val shape = MaterialTheme.shapes.extraSmall
    Row(
        modifier = Modifier
            .fillMaxSize()
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .border(border, shape)
            .padding(start = 5.dp, end = 1.dp),
        verticalAlignment = CenterVertically
    ) {
        Text(
            text = selectedItem.text,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSecondary,
            overflow = Ellipsis,
            maxLines = 1,
            style = MaterialTheme.typography.bodyMedium
        )
        Icon(
            painter = painterResource(Res.drawable.drop_down_arrow),
            tint = MaterialTheme.colorScheme.onSecondary,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
                .background(iconColor, shape)
                .border(iconBorder, shape)
        )
    }
}

@Composable
private fun <T> DropDownMenuContent(
    selectedItem: DropDownMenuItem<T>,
    items: List<DropDownMenuItem<T>>,
    width: Dp,
    onChangeValue: (T) -> Unit
) {
    val shape = MaterialTheme.shapes.extraSmall
    Surface(
        modifier = Modifier.shadow(
            elevation = 2.dp,
            shape = shape
        ),
        color = MaterialTheme.colorScheme.secondary,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.background),
        shape = shape
    ) {
        Column(
            modifier = Modifier.padding(2.dp).width(width),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            DropDownMenuOption(true, selectedItem.text) { onChangeValue(selectedItem.value) }
            Divider(
                Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                MaterialTheme.colorScheme.background
            )
            items.filter { it.value != selectedItem.value }
                .forEach { item ->
                    DropDownMenuOption(false, item.text) { onChangeValue(item.value) }
                }
        }
    }
}

@Composable
private fun DropDownMenuOption(
    selected: Boolean,
    text: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val contentColor = MaterialTheme.colorScheme.run { if (hovered) onPrimary else onSecondary }
    val containerColor = MaterialTheme.colorScheme.run { if (hovered) primary else secondary }
    val shape = MaterialTheme.shapes.extraSmall
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(
                color = containerColor,
                shape = shape
            )
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .pointerHoverIcon(Hand)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = CenterVertically
    ) {
        if (selected) {
            Icon(
                painter = painterResource(Res.drawable.check),
                tint = contentColor,
                contentDescription = null,
                modifier = Modifier.size(15.dp)
            )
        } else {
            Spacer(Modifier.width(15.dp))
        }
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun rememberPositionProvider(shiftLeft: Dp): PopupPositionProvider {
    val shiftLeftPx = with(LocalDensity.current) { shiftLeft.roundToPx() }
    return remember(shiftLeftPx) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset = IntOffset(anchorBounds.left - shiftLeftPx, anchorBounds.top)
        }
    }
}
