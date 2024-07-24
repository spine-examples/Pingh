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

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.sp

/**
 * UI theme for the application.
 *
 * @see [MaterialTheme]
 */
@Composable
internal fun PinghTheme(content: @Composable () -> Unit): Unit = MaterialTheme(
    colorScheme = colorScheme,
    typography = typography,
    content = content
)

/**
 * The light colors scheme of the application.
 */
@Suppress("MagicNumber") // Colors are defined using RGB components.
private val colorScheme = lightColorScheme(
    primary = Color(162, 215, 245),
    onPrimary = Color.White,
    primaryContainer = Color(59, 155, 255),
    secondary = Color.White,
    onSecondary = Color(40, 40, 40),
    tertiary = Color(150, 150, 150),
    background = Color(235, 235, 235),
    onBackground = Color(213, 213, 213),
    error = Color(255, 161, 161),
    errorContainer = Color(255, 232, 232)
)

/**
 * Loaded from the resource San Francisco font family.
 */
private val sanFrancisco = FontFamily(
    Font(
        "fonts/SFUIDisplay-Bold.ttf",
        FontWeight.Bold,
        FontStyle.Normal
    ),
    Font(
        "fonts/SFUIDisplay-Semibold.ttf",
        FontWeight.SemiBold,
        FontStyle.Normal
    ),
    Font(
        "fonts/SFUIText-Bold.ttf",
        FontWeight.Bold,
        FontStyle.Normal
    ),
    Font(
        "fonts/SFUIText-Italic.ttf",
        FontWeight.Normal,
        FontStyle.Italic
    ),
    Font(
        "fonts/SFUIText-Regular.ttf",
        FontWeight.Normal,
        FontStyle.Normal
    )
)

/**
 * Text styles of the application.
 */
private val typography = Typography(
    displayLarge = TextStyle(
        fontFamily = sanFrancisco,
        fontSize = 16.sp
    ),
    displayMedium = TextStyle(
        fontFamily = sanFrancisco,
        fontSize = 14.sp
    ),
    displaySmall = TextStyle(
        fontFamily = sanFrancisco,
        fontSize = 12.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = sanFrancisco,
        fontSize = 12.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = sanFrancisco,
        fontSize = 10.sp
    ),
    bodySmall = TextStyle(
        fontFamily = sanFrancisco,
        fontSize = 8.sp
    )
)
