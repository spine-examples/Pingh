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

import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.milliseconds

/**
 * Creates an animation for three points that alternately change their position along the y-axis
 * and return to their initial position.
 *
 * The animation loops indefinitely. It starts when the item enters the composition
 * and stops when the item leaves.
 *
 * @param color The color of the dots.
 * @param dotSize The size of a dot.
 * @param spaceBetweenDots The distance between dots.
 * @param maxOffset The maximum change in a dot's position along the y-axis.
 * @param duration The animation duration.
 */
@Composable
@Suppress("MagicNumber" /* Numbers are used to define the keyframes of the animation. */)
internal fun DotsTyping(
    color: Color,
    dotSize: Dp = 7.dp,
    spaceBetweenDots: Dp = 6.dp,
    maxOffset: Float = 10f,
    duration: Duration = 1000.milliseconds
) {
    val infiniteTransition = rememberInfiniteTransition()
    val frame = duration / 4

    val offset1 by infiniteTransition.animateDotOffset(ZERO, duration, frame, maxOffset)
    val offset2 by infiniteTransition.animateDotOffset(frame, duration, frame, maxOffset)
    val offset3 by infiniteTransition.animateDotOffset(frame * 2, duration, frame, maxOffset)

    Row(
        modifier = Modifier.padding(top = maxOffset.dp),
        verticalAlignment = CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spaceBetweenDots, CenterHorizontally)
    ) {
        Dot(dotSize, offset1, color)
        Dot(dotSize, offset2, color)
        Dot(dotSize, offset3, color)
    }
}

/**
 * Creates an animation of float type for [Dot]'s offset that runs infinitely.
 */
@Composable
private fun InfiniteTransition.animateDotOffset(
    delay: Duration,
    duration: Duration,
    frame: Duration,
    maxOffset: Float
): State<Float> =
    animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = duration.intMillis
                0f at delay.intMillis using LinearEasing
                maxOffset at (delay + frame).intMillis using LinearEasing
                0f at (delay + frame * 2).intMillis
            }
        )
    )

/**
 * The value of this duration expressed as a `Int` number of milliseconds.
 */
private val Duration.intMillis
    get() = inWholeMilliseconds.toInt()

/**
 * Displays a dot.
 */
@Composable
private fun Dot(size: Dp, offset: Float, color: Color) {
    Spacer(
        Modifier
            .size(size)
            .offset(y = -offset.dp)
            .background(
                color = color,
                shape = CircleShape
            )
    )
}
