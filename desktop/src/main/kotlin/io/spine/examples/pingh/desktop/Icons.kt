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
import androidx.compose.ui.graphics.painter.BitmapPainter
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
internal object Icons {
    internal val pingh: BitmapPainter =
        BitmapPainter(useResource("icons/pingh.png", ::loadImageBitmap))
    internal val snooze: BitmapPainter =
        BitmapPainter(useResource("icons/snooze.png", ::loadImageBitmap))
    internal val refresh: BitmapPainter =
        BitmapPainter(useResource("icons/refresh.png", ::loadImageBitmap))
    internal val back: BitmapPainter =
        BitmapPainter(useResource("icons/back.png", ::loadImageBitmap))
    internal val logout: BitmapPainter =
        BitmapPainter(useResource("icons/logout.png", ::loadImageBitmap))
}
