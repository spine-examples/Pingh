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

@file:Suppress("TooManyFunctions") //

package io.spine.examples.pingh.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.spine.examples.pingh.client.DesktopClient
import io.spine.examples.pingh.github.Username
import kotlin.reflect.KClass

/**
 * Max length of a GitHub username.
 */
private const val maxLengthOfUsername = 39

/**
 * Displays a login form.
 *
 * If the username is entered correct, user will be [logged in][DesktopClient.logIn] into
 * the Pingh server and redirected to the [MentionsPage].
 */
@Composable
internal fun LoginPage(
    client: DesktopClient,
    toMentionsPage: () -> Unit
) {
    val username = remember {
        mutableStateOf(Username::class.buildWithoutValidationBy(""))
    }
    val isError = remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        UsernameInput(
            value = username.value.value,
            onChange = { value ->
                username.value = Username::class.buildWithoutValidationBy(value)
                isError.value = !username.value.validate()
            },
            isError = isError.value
        )
        Spacer(Modifier.height(20.dp))
        LoginButton(
            enabled = !isError.value
        ) {
            client.logIn(username.value) {
                toMentionsPage()
            }
        }
    }
}

/**
 * Displays a username input field on the login form.
 */
@Composable
private fun UsernameInput(
    value: String,
    onChange: (String) -> Unit,
    isError: Boolean
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val borderWidth = if (isFocused) 2.dp else 1.dp
    val borderColor = when {
        isError -> MaterialTheme.colorScheme.error
        isFocused -> MaterialTheme.colorScheme.secondary
        else -> Color.Black
    }
    BasicTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier
            .width(180.dp)
            .height(52.dp),
        textStyle = MaterialTheme.typography.bodyLarge,
        interactionSource = interactionSource,
        singleLine = true
    ) { innerTextField ->
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            InnerBox(
                border = BorderStroke(borderWidth, borderColor)
            ) {
                InputContainer(
                    value = value,
                    textField = innerTextField,
                    modifier = Modifier.weight(1f)
                )
            }
            Label(borderColor)
            ErrorMesage(isError)
        }
    }
}

/**
 * Inner box for the text input design.
 */
@Composable
private fun InnerBox(
    border: BorderStroke,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .width(180.dp)
            .height(40.dp)
            .border(
                border = border,
                shape = MaterialTheme.shapes.medium
            )
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.medium
            )
            .padding(
                horizontal = 10.dp,
                vertical = 3.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

/**
 * Container for the text field.
 */
@Composable
private fun InputContainer(
    value: String,
    textField: @Composable () -> Unit,
    modifier: Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart,
    ) {
        Placeholder(value.isEmpty())
        textField()
    }
}

/**
 * The label which placed on the top border of the text field.
 */
@Composable
private fun Label(color: Color) {
    Box(
        modifier = Modifier
            .width(90.dp)
            .height(10.dp)
            .absoluteOffset(x = 10.dp, y = (-5).dp)
    ) {
        Text(
            text = "GitHub username",
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primary),
            color = color,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * The placeholder which placed inside the text field.
 */
@Composable
private fun Placeholder(
    isShown: Boolean
) {
    if (isShown) {
        Text(
            text = "john-smith",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * The error message which is placed below the text field.
 */
@Composable
private fun ErrorMesage(isShown: Boolean) {
    if (isShown) {
        Text(
            text = "Must consist of alphanumeric characters and dashes, " +
                    "without consecutive dashes or dashes at the beginning or end.",
            modifier = Modifier
                .width(155.dp)
                .height(30.dp)
                .absoluteOffset(x = 15.dp, y = 42.dp),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

/**
 * Displays a `Button` on the login form.
 */
@Composable
private fun LoginButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .width(120.dp)
            .height(40.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Text(
            text = "Login",
            style = MaterialTheme.typography.displayMedium
        )
    }
}

/**
 * Creates a new `Username` with the specified value without validation.
 *
 * The `Username` value cannot be empty. However, if the login field is empty,
 * the `Username` will also be empty. To avoid exceptions during building,
 * the `buildPartial` method is used, which does not check the `Username` value.
 *
 * Before sending this `Username` to the server, ensure to [validate] the value.
 */
private fun KClass<Username>.buildWithoutValidationBy(value: String) =
    Username.newBuilder()
        .setValue(value)
        .buildPartial()

/**
 * Returns `true` if the `Username` is valid according to GitHub criteria; otherwise, returns `false`.
 *
 * A valid GitHub username must:
 *
 * - Consist of alphanumeric characters and dashes (`'-'`);
 * - Not have consecutive dashes or dashes at the beginning or end;
 * - Not exceed 39 characters.
 *
 * @see <a href="https://docs.github.com/en/enterprise-server@3.9/admin/managing-iam/iam-configuration-reference/username-considerations-for-external-authentication">
 *     Username considerations for external authentication</a>
 */
private fun Username.validate(): Boolean {
    if (this.value.length !in 1..maxLengthOfUsername) {
        return false
    }
    var previous = '-'
    this.value.forEach { current ->
        if (previous == '-' && current == '-') {
            return false
        }
        if (!current.isAlphanumeric() && current != '-') {
            return false
        }
        previous = current
    }
    return previous != '-'
}

/**
 * Returns `true` if the character is a digit or an English letter in any case;
 * otherwise, returns `false`.
 */
private fun Char.isAlphanumeric(): Boolean =
    this in 'A'..'Z' || this in 'a'..'z' || this.isDigit()
