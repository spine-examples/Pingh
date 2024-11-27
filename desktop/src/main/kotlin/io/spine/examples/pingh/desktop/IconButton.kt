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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import java.awt.Cursor

/**
 * The proportion of the button's size that the icon occupies.
 * Since the button is circular, the icon should be scaled down
 * to avoid truncation at the edges.
 */
private const val circularIconSizeMultiplier = 0.75f

/**
 * Displays a round button that contains icon.
 *
 * @param icon The painter to draw icon.
 * @param onClick Called when this icon button is clicked.
 * @param modifier The modifier to be applied to this icon button.
 * @param enabled Controls the enabled state of this icon button.
 * @param shape The shape of this icon button's container.
 * @param colors The `IconButtonColors` that is used to resolve the colors used for this icon button
 *   in different states.
 * @param tooltip The text to be displayed in the tooltip. If `null`, no tooltip is shown.
 * @param sizeMultiplier The proportion of the button's size that the icon occupies.
 */
@Composable
@Suppress("LongParameterList" /* For detailed customization. */)
internal fun IconButton(
    icon: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CircleShape,
    colors: IconButtonColors = IconButtonDefaults.filledIconButtonColors(),
    tooltip: String? = null,
    sizeMultiplier: Float = circularIconSizeMultiplier
) {
    val wrapper = if (tooltip != null) wrapInTooltipBox(tooltip) else noWrap()
    wrapper {
        FilledIconButton(
            onClick = onClick,
            modifier = modifier
                .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))),
            enabled = enabled,
            shape = shape,
            colors = colors
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(sizeMultiplier),
                tint = colors.contentColor
            )
        }
    }
}

/**
 * Returns composable function that wraps the content within
 * a [tooltip] area using [TooltipBox].
 *
 * @param tooltip The text to be displayed in the tooltip.
 */
@OptIn(ExperimentalMaterial3Api::class)
private fun wrapInTooltipBox(
    tooltip: String
): @Composable (content: @Composable () -> Unit) -> Unit =
    { content ->
        TooltipBox(
            positionProvider = rememberPositionProvider(4.dp),
            tooltip = { TooltipContent(tooltip) },
            state = rememberTooltipState(),
            content = content
        )
    }

/**
 * Creates a `PopupPositionProvider` to calculate the tooltip's coordinates
 * based on the anchor data.
 *
 * The tooltip is positioned below the anchor by default, shifting to the right
 * if the tooltip content fits within the screen. If there isn't enough space below,
 * the tooltip appears above the anchor. If there isn't enough space on the right,
 * the tooltip shifts to the left.
 *
 * @param tooltipAnchorSpacing The spacing between the tooltip and the anchor content.
 */
@Composable
private fun rememberPositionProvider(tooltipAnchorSpacing: Dp = 0.dp): PopupPositionProvider {
    val tooltipAnchorSpacingPx = with(LocalDensity.current) {
        tooltipAnchorSpacing.roundToPx()
    }
    return remember(tooltipAnchorSpacingPx) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset {
                var x = anchorBounds.left
                if (x + popupContentSize.width > windowSize.width) {
                    x = anchorBounds.right - popupContentSize.width
                }
                var y = anchorBounds.bottom + tooltipAnchorSpacingPx
                if (y + popupContentSize.height > windowSize.height) {
                    y = anchorBounds.top - popupContentSize.height - tooltipAnchorSpacingPx
                }
                return IntOffset(x, y)
            }
        }
    }
}

/**
 * Displays the tooltip's content.
 *
 * @param text The text to be displayed in the tooltip.
 */
@Composable
private fun TooltipContent(text: String) {
    val shape = MaterialTheme.shapes.small
    Surface(
        modifier = Modifier.shadow(
            elevation = 4.dp,
            shape = shape
        ),
        color = MaterialTheme.colorScheme.secondary,
        shape = shape
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(10.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * Returns a function that executes the given composable function.
 */
private fun noWrap(): @Composable (content: @Composable () -> Unit) -> Unit = { it() }
