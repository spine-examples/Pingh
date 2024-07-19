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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.spine.examples.pingh.client.DesktopClient
import io.spine.examples.pingh.github.Username
import io.spine.examples.pingh.github.buildBy

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
    val state = remember { UsernameState() }
    val isError = remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Input(
            value = state.username(),
            onChange = {
                state.set(it)
                isError.value = state.wasChanged() && !state.validate()
            },
            isError = isError.value
        )
        Spacer(Modifier.height(20.dp))
        LoginButton {
            if (state.validate()) {
                val username = Username::class.buildBy(state.username())
                client.logIn(username) {
                    toMentionsPage()
                }
            } else {
                isError.value = true
            }
        }
    }
}

/**
 * Displays a username input field on the login form.
 *
 * In addition, it checks the correctness of the entered data. If the username
 * has been [specified][UsernameState.wasChanged] but is not [valid][UsernameState.validate],
 * the value is considered incorrect, so the field signals an input error
 * and shows a hint.
 */
@Composable
private fun LoginInput(
    state: UsernameState,
    isError: MutableState<Boolean>
) {
    OutlinedTextField(
        value = state.username(),
        onValueChange = {
            state.set(it)
            isError.value = state.wasChanged() && !state.validate()
        },
        modifier = Modifier
            .width(180.dp)
            .height(45.dp)
            .padding(0.dp),
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            fontWeight = FontWeight.Normal
        ),
        label = {
            Text(
                text = "GitHub username",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        placeholder = {
            Text(
                text = "jonh-smith",
                color = Color.Gray,
                style = MaterialTheme.typography.bodyLarge
            )
        },
        supportingText = {
            if (isError.value) {
                Text(
                    text = state.errorMessage(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontStyle = FontStyle.Italic
                    )
                )
            }
        },
        isError = isError.value,
        singleLine = true,
        shape = MaterialTheme.shapes.large,
        colors = TextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.primary,
            focusedContainerColor = MaterialTheme.colorScheme.primary,
            errorContainerColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = Color.Black,
            focusedLabelColor = Color.Black,
            focusedIndicatorColor = Color.Black
        )
    )
}

/**
 * Displays a `Button` on the login form.
 */
@Composable
private fun LoginButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .width(120.dp)
            .height(40.dp),
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
 * State of GitHub username in the [LoginPage].
 */
private class UsernameState {

    private val regex = Regex("""[a-zA-Z\d-]+""")

    /**
     * Value of the username.
     */
    private val username: MutableState<String> = mutableStateOf("")

    /**
     * Information on whether there has been a change in username state.
     */
    private var wasChanged: Boolean = false

    /**
     * Returns the username value.
     */
    internal fun username() = username.value

    /**
     * Sets a new username value and specifies that this state was changed.
     */
    internal fun set(value: String) {
        if (!wasChanged && value.isNotEmpty()) {
            wasChanged = true
        }
        username.value = value
    }

    /**
     * Checks that the username value is not empty.
     */
    internal fun validate(): Boolean = username.value.isNotEmpty()

    /**
     * Returns information on whether there has been a change in username state.
     */
    internal fun wasChanged(): Boolean = wasChanged

    /**
     * Returns the error message if state value is invalid.
     */
    internal fun errorMessage(): String = if (validate()) "" else {
        "Username must be specified"
    }
}


@Composable
public fun Input(
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
                    textField = { innerTextField() },
                    modifier = Modifier.weight(1f)
                )
            }
            Label(borderColor)
            Tip(isError)
        }
    }
}

/**
 * Inner box for all content of the `Input`.
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
        Placeholder(
            isShown = value.isEmpty(),
            value = "john-smith"
        )
        textField()
    }
}

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
            textAlign = TextAlign.Center,
            color = color,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * The placeholder to be shown inside the text field.
 */
@Composable
private fun Placeholder(
    isShown: Boolean,
    value: String
) {
    if (isShown) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun Tip(isShown: Boolean) {
    if (isShown) {
        Text(
            text = "Must be specified",
            modifier = Modifier
                .width(90.dp)
                .height(10.dp)
                .absoluteOffset(x = 13.dp, y = 42.dp),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
