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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults.iconButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.PointerIcon.Companion.Hand
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Displays an icon button that displays the menu when clicked.
 *
 * The menu is located at the bottom of the icon, shifting to the left.
 *
 * @param icon The painter to draw icon.
 * @param modifier The modifier to be applied to icon button.
 * @param tooltip The text to be displayed in the tooltip.
 *   If `null`, no tooltip is shown.
 * @param iconFraction The proportion of the button's size that the icon occupies.
 * @param items Menu items displayed inside the menu.
 */
@Composable
internal fun Menu(
    icon: Painter,
    modifier: Modifier = Modifier,
    tooltip: String? = null,
    iconFraction: Float = 0.85f,
    items: @Composable MenuScope.() -> Unit
) {
    val scope = remember { MenuScopeImpl() }
    var isShown by scope.isShown
    var isTogglingAllowed by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
    Box {
        IconButton(
            icon = icon,
            onClick = {
                if (isTogglingAllowed) {
                    isShown = !isShown
                }
            },
            modifier = modifier,
            colors = iconButtonColors(
                contentColor = MaterialTheme.colorScheme.onSecondary
            ),
            tooltip = tooltip,
            sizeFraction = iconFraction
        )
        if (isShown) {
            Popup(
                popupPositionProvider = rememberPositionProvider(4.dp),
                onDismissRequest = {
                    isTogglingAllowed = false
                    isShown = false
                    coroutineScope.launch {
                        delay(200.milliseconds)
                        isTogglingAllowed = true
                    }
                },
                content = { MenuContent(scope, items) }
            )
        }
    }
}

/**
 * Displays the menu items.
 */
@Composable
private fun MenuContent(scope: MenuScope, items: @Composable MenuScope.() -> Unit) {
    val shape = MaterialTheme.shapes.small
    Surface(
        modifier = Modifier.shadow(
            elevation = 4.dp,
            shape = shape
        ),
        color = MaterialTheme.colorScheme.secondary,
        shape = shape
    ) {
        Column(
            modifier = Modifier.padding(5.dp).width(150.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            items(scope)
        }
    }
}

/**
 * Scope for [Menu] items.
 */
internal interface MenuScope {

    /**
     * Adds an item to the menu.
     *
     * Clicking on this item closes the menu before executing [onClick] action.
     *
     * @param text The text describing this item.
     * @param modifier The modifier to be applied to this item.
     * @param leadingIcon The icon that is placed in front of the text.
     * @param onClick Called when the item is clicked.
     */
    @Composable
    fun MenuItem(
        text: String,
        modifier: Modifier,
        leadingIcon: Painter,
        onClick: () -> Unit
    )
}

private class MenuScopeImpl : MenuScope {
    /**
     * Whether the menu is displayed.
     */
    val isShown = mutableStateOf(false)

    /**
     * @see [MenuScope.MenuItem]
     */
    @Composable
    override fun MenuItem(
        text: String,
        modifier: Modifier,
        leadingIcon: Painter,
        onClick: () -> Unit
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val isHovered by interactionSource.collectIsHoveredAsState()
        val contentColor = if (isHovered) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSecondary
        }
        val containerColor = if (isHovered) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.secondary
        }
        val shape = MaterialTheme.shapes.small
        Row(
            modifier = modifier.fillMaxWidth()
                .background(
                    color = containerColor,
                    shape = shape
                )
                .hoverable(interactionSource)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {
                        isShown.value = false
                        onClick()
                    }
                )
                .pointerHoverIcon(Hand)
                .padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = CenterVertically
        ) {
            Icon(
                painter = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = contentColor
            )
            Text(
                text = text,
                color = contentColor,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * Creates a `PopupPositionProvider` to calculate the menu's coordinates
 * based on the anchor data.
 *
 * The menu is positioned below the anchor, shifting to the left.
 *
 * @param menuAnchorSpacing The spacing between the menu and the anchor content.
 */
@Composable
private fun rememberPositionProvider(menuAnchorSpacing: Dp): PopupPositionProvider {
    val menuAnchorSpacingPx = with(LocalDensity.current) {
        menuAnchorSpacing.roundToPx()
    }
    return remember(menuAnchorSpacingPx) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset =
                IntOffset(
                    anchorBounds.right - popupContentSize.width,
                    anchorBounds.bottom + menuAnchorSpacingPx
                )
        }
    }
}
