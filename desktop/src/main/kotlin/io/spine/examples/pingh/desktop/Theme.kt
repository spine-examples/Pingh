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
import androidx.compose.ui.unit.sp
import io.spine.example.pingh.desktop.generated.resources.Res
import io.spine.example.pingh.desktop.generated.resources.SFUIDisplay_Semibold
import io.spine.example.pingh.desktop.generated.resources.SFUIText_Bold
import io.spine.example.pingh.desktop.generated.resources.SFUIText_Italic
import io.spine.example.pingh.desktop.generated.resources.SFUIText_Regular
import org.jetbrains.compose.resources.Font

/**
 * UI theme for the application.
 *
 * @see [MaterialTheme]
 */
@Composable
internal fun Theme(content: @Composable () -> Unit): Unit = MaterialTheme(
    colorScheme = colorScheme,
    typography = typography(sanFrancisco()),
    content = content
)

/**
 * The light colors scheme of the application.
 */
@Suppress("MagicNumber" /* Colors are defined using RGB components. */)
private val colorScheme = lightColorScheme(
    primary = Color(9, 105, 218),
    onPrimary = Color.White,
    primaryContainer = Color(59, 155, 255),
    secondary = Color.White,
    onSecondary = Color(40, 40, 40),
    secondaryContainer = Color(150, 150, 150),
    onSecondaryContainer = Color(80, 80, 80),
    background = Color(235, 235, 235),
    onBackground = Color(213, 213, 213),
    surface = Color(120, 120, 120),
    error = Color(255, 161, 161)
)

/**
 * Loads from the resource San Francisco font family.
 */
@Composable
private fun sanFrancisco(): FontFamily = FontFamily(
    Font(
        Res.font.SFUIText_Bold,
        FontWeight.Bold,
        FontStyle.Normal
    ),
    Font(
        Res.font.SFUIDisplay_Semibold,
        FontWeight.SemiBold,
        FontStyle.Normal
    ),
    Font(
        Res.font.SFUIText_Bold,
        FontWeight.Bold,
        FontStyle.Normal
    ),
    Font(
        Res.font.SFUIText_Italic,
        FontWeight.Normal,
        FontStyle.Italic
    ),
    Font(
        Res.font.SFUIText_Regular,
        FontWeight.Normal,
        FontStyle.Normal
    )
)

/**
 * Text styles of the application.
 */
private fun typography(family: FontFamily) = Typography(
    displayLarge = TextStyle(
        fontFamily = family,
        fontSize = 18.sp
    ),
    displayMedium = TextStyle(
        fontFamily = family,
        fontSize = 16.sp
    ),
    displaySmall = TextStyle(
        fontFamily = family,
        fontSize = 14.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = family,
        fontSize = 14.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = family,
        fontSize = 12.sp
    ),
    bodySmall = TextStyle(
        fontFamily = family,
        fontSize = 10.sp
    )
)
