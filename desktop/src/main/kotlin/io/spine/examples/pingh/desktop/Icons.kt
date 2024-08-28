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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import java.awt.Cursor

/**
 * The proportion of the button's size that the icon occupies.
 * Since the button is circular, the icon should be scaled down
 * to avoid truncation at the edges.
 */
private const val iconSizeMultiplier = 0.75f

/**
 * Displays a round button that contains icon.
 *
 * @param icon The painter to draw icon.
 * @param onClick Called when this icon button is clicked.
 * @param modifier The modifier to be applied to this icon button.
 * @param colors The `IconButtonColors` that is used to resolve the colors used for this icon button
 *   in different states.
 */
@Composable
internal fun IconButton(
    icon: BitmapPainter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors()
) {
    FilledIconButton(
        onClick = onClick,
        modifier = modifier
            .clip(CircleShape)
            .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))),
        colors = colors
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(iconSizeMultiplier),
            tint = colors.contentColor
        )
    }
}

/**
 * Icons used in the Pingh desktop client.
 *
 * These icons are [free](https://bit.ly/3Tjmsqg) for personal and commercial
 * use with attribution.
 *
 * Created by [Icon Hubs](https://www.flaticon.com/authors/icon-hubs).
 */
@Suppress("MagicNumber" /* Colors are defined using RGB components. */)
internal object Icons {
    internal val pingh: BitmapPainter =
        BitmapPainter(useResource("icons/pingh.png", ::loadImageBitmap))
    internal val snooze: BitmapPainter =
        BitmapPainter(useResource("icons/snooze.png", ::loadImageBitmap))
    internal val refresh: BitmapPainter =
        BitmapPainter(useResource("icons/refresh.png", ::loadImageBitmap))
    internal val back: BitmapPainter =
        BitmapPainter(useResource("icons/back.png", ::loadImageBitmap))
    internal val copy: BitmapPainter =
        BitmapPainter(useResource("icons/copy.png", ::loadImageBitmap))
    internal val trayWhite: Painter =
        ColorBitmapPainter("icons/tray.png", Color(232, 232, 232))
    internal val trayBlack: Painter =
        ColorBitmapPainter("icons/tray.png", Color(40, 40, 40))
}

/**
 * A `Painter` implementation used to draw an bitmap image with the passed `color`.
 *
 * @param resourcePath The path to the image resource.
 * @property color The color applied to the image.
 */
internal class ColorBitmapPainter(
    resourcePath: String,
    private val color: Color
) : Painter() {

    /**
     * Bitmap image loaded from resource.
     */
    private val img = BitmapPainter(useResource(resourcePath, ::loadImageBitmap))

    override val intrinsicSize: Size
        get() = img.intrinsicSize

    override fun DrawScope.onDraw() {
        with(img) {
            draw(size, colorFilter = ColorFilter.tint(color))
        }
    }
}
