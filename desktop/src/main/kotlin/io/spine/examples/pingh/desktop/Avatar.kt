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

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.spine.examples.pingh.github.Username
import io.spine.net.Url

/**
 * Displays a round avatar with an image loaded from the specified URL.
 *
 * The image loads asynchronously.
 */
@Composable
internal fun Avatar(
    url: Url,
    modifier: Modifier = Modifier
) {
    AsyncImage(
        model = url.spec,
        contentDescription = null,
        modifier = modifier
            .clip(CircleShape)
    )
}

/**
 * Displays a round avatar with the first letter of the provided name
 * on a gradient background.
 */
@Composable
internal fun Avatar(
    name: Username,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val gradient = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.primaryContainer
    )
    Box(
        contentAlignment = Alignment.Center
    ) {
        Image(
            modifier = modifier
                .size(size)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
            contentDescription = null,
            painter = object : Painter() {
                override val intrinsicSize: Size = Size(size.value, size.value)
                override fun DrawScope.onDraw() {
                    drawRect(
                        brush = Brush.linearGradient(gradient),
                        size = Size(size.value * 2, size.value * 2)
                    )
                }
            }
        )
        Text(
            text = if(name.value.isNotEmpty()) name.value[0].toString() else "",
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = (size.value / 2).sp,
            style = MaterialTheme.typography.displayLarge
        )
    }
}
