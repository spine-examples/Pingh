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
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp

@Composable
internal fun Input(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    rule: (String) -> Boolean = { true },
    tipMessage: String = "",
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentPadding: PaddingValues = PaddingValues(16.dp, 8.dp)
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val borderColor = if (isFocused) MaterialTheme.colorScheme.secondary else Color.Unspecified
    BasicTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier,
        textStyle = MaterialTheme.typography.bodyMedium,
        interactionSource = interactionSource,
        maxLines = 1
    ) { innerTextField ->
        InnerBox(
            borderColor = borderColor,
            containerColor = containerColor,
            contentPadding = contentPadding
        ) {
            InputContainer(
                value = value,
                placeholder = placeholder,
                textField = innerTextField,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Inner box for all content of the `Input`.
 */
@Composable
private fun InnerBox(
    borderColor: Color,
    containerColor: Color,
    contentPadding: PaddingValues,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .border(
                width = 1.dp,
                color = borderColor,
                shape = MaterialTheme.shapes.small
            )
            .background(
                color = containerColor,
                shape = MaterialTheme.shapes.small
            )
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
private fun InputContainer(
    value: String,
    placeholder: String,
    textField: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.CenterStart
    ) {
        Placeholder(
            isShown = value.isEmpty(),
            value = placeholder
        )
        textField()
    }
}

@Composable
private fun Placeholder(
    isShown: Boolean,
    value: String
) {
    if(isShown) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontStyle = FontStyle.Normal
            ),
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
private fun Tooltip(
    message: String,
    isError: Boolean,
    modifier: Modifier
) {
    val color = if (isError) MaterialTheme.colorScheme.error
    else MaterialTheme.colorScheme.onPrimary
    if(message.isNotEmpty()) {

    }
}
