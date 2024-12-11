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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider

@Composable
internal fun Menu(
    icon: Painter,
    iconSize: Dp = 30.dp,
    tooltip: String? = null,
    iconFraction: Float = 0.85f,
    items: @Composable MenuScope.() -> Unit
) {
    val scope = remember { MenuScopeImpl() }
    var isShown by scope.isShown
    Box {
        IconButton(
            icon = icon,
            onClick = { isShown = !isShown },
            modifier = Modifier.size(iconSize),
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
                    isShown = false
                },
            ) {
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
        }
    }
}

internal interface MenuScope {

    @Composable
    fun MenuItem(
        text: String,
        leadingIcon: Painter,
        onClick: () -> Unit
    )
}

private class MenuScopeImpl : MenuScope {

    val isShown = mutableStateOf(false)

    @Composable
    override fun MenuItem(text: String, leadingIcon: Painter, onClick: () -> Unit) {
        val interactionSource = remember { MutableInteractionSource() }
        val isHovered by interactionSource.collectIsHoveredAsState()
        val content = if (isHovered) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSecondary
        }
        val container = if (isHovered) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.secondary
        }
        val shape = MaterialTheme.shapes.small
        Row(
            modifier = Modifier.fillMaxWidth()
                .background(
                    color = container,
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
                .padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = CenterVertically
        ) {
            Icon(
                painter = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = content
            )
            Text(
                text = text,
                color = content,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

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
