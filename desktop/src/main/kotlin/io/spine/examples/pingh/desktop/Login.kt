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

@file:Suppress("TooManyFunctions") // Using Compose requires many functions to render the UI.

package io.spine.examples.pingh.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.protobuf.Duration
import io.spine.examples.pingh.client.LoginFlow
import io.spine.examples.pingh.client.LoginState
import io.spine.examples.pingh.github.UserCode
import io.spine.examples.pingh.github.Username
import io.spine.examples.pingh.github.buildBy
import io.spine.examples.pingh.github.validateUsernameValue
import io.spine.net.Url
import io.spine.protobuf.Durations2.toMinutes

/**
 * Displays the page with the current login step.
 *
 * Initially, the user must enter their `Username`, after which they will receive
 * a code that must be entered into GitHub. After entering the code, the user needs
 * to confirm the login on the application page.
 *
 * @param flow the control flow for the user login process.
 * @param toMentionsPage the navigation to the 'Mentions' page.
 */
@Composable
internal fun LoginPage(
    flow: LoginFlow,
    toMentionsPage: () -> Unit
) {
    val state by flow.state.collectAsState()
    when (state) {
        LoginState.USERNAME_ENTERING -> UsernameEnteringPage(
            flow = flow
        )

        LoginState.VERIFICATION -> VerificationPage(
            flow = flow,
            toMentionsPage = toMentionsPage
        )
    }
}

/**
 * Displays a login form.
 *
 * If the `Username` is entered correctly, the user will receive the `UserCode` and
 * be redirected to the login verification page.
 * [LoginButton] is not enable while the entered `Username` is invalid.
 *
 * @param flow the control flow for the user login process.
 */
@Composable
private fun UsernameEnteringPage(
    flow: LoginFlow
) {
    var username by remember { mutableStateOf("") }
    var wasChanged by remember { mutableStateOf(false) }
    val isError = remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.secondary),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ApplicationInfo()
        Spacer(Modifier.height(35.dp))
        UsernameInput(
            value = username,
            onChange = { value ->
                username = value
                wasChanged = true
            },
            isError = isError
        )
        Spacer(Modifier.height(10.dp))
        LoginButton(
            enabled = wasChanged && !isError.value
        ) {
            val name = Username::class.buildBy(username)
            flow.requestUserCode(name)
        }
    }
}

/**
 * Displays application information, including the name, icon, and a description explaining
 * why the application uses authentication via GitHub.
 */
@Composable
private fun ApplicationInfo() {
    Column(
        modifier = Modifier.padding(horizontal = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = Icons.pingh,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSecondary
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Pingh",
                modifier = Modifier.width(100.dp),
                color = MaterialTheme.colorScheme.onSecondary,
                fontSize = 26.sp,
                style = MaterialTheme.typography.displayLarge
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Pingh is a GitHub app that looks up mentions on behalf of the user. " +
                    "It requires authentication via GitHub.",
            modifier = Modifier.width(180.dp),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * Displays a username input field on the login form.
 *
 * @param value the current value of the input field.
 * @param onChange called when input value is changed.
 * @param isError indicates if the input's current value is in error.
 */
@Composable
private fun UsernameInput(
    value: String,
    onChange: (String) -> Unit,
    isError: MutableState<Boolean>
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val borderWidth = if (isFocused) 2.dp else 1.dp
    val borderColor = when {
        isError.value -> MaterialTheme.colorScheme.error
        isFocused -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    BasicTextField(
        value = value,
        onValueChange = { changedValue ->
            onChange(changedValue)
            isError.value = !validateUsernameValue(changedValue)
        },
        modifier = Modifier
            .width(180.dp)
            .height(52.dp),
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSecondary
        ),
        interactionSource = interactionSource,
        singleLine = true
    ) { innerTextField ->
        Box(
            Modifier.fillMaxSize()
        ) {
            InputContainer(
                value = value,
                textField = innerTextField,
                border = BorderStroke(borderWidth, borderColor)
            )
            Label(borderColor)
            ErrorMesage(isError.value)
        }
    }
}

/**
 * Displays a container for the text field.
 *
 * @param value the current value of the input field.
 * @param textField the composable function that displays the content of an input field.
 * @param border the border of this input.
 */
@Composable
private fun InputContainer(
    value: String,
    textField: @Composable () -> Unit,
    border: BorderStroke
) {
    Row(
        modifier = Modifier
            .width(180.dp)
            .height(40.dp)
            .border(border = border, shape = MaterialTheme.shapes.medium)
            .background(
                color = MaterialTheme.colorScheme.secondary,
                shape = MaterialTheme.shapes.medium
            )
            .padding(horizontal = 10.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.CenterStart,
        ) {
            Placeholder(value.isEmpty())
            textField()
        }
    }
}

/**
 * Displays a label which placed on the top border of the text field.
 *
 * @param color the color of this label.
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
                .background(MaterialTheme.colorScheme.secondary),
            color = color,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * Displays a placeholder which placed inside the text field.
 *
 * @param isShown indicates whether the placeholder is displayed.
 */
@Composable
private fun Placeholder(isShown: Boolean) {
    if (isShown) {
        Text(
            text = "john-smith",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * Displays an error message which is placed below the text field.
 *
 * @param isShown indicates whether the error message is displayed.
 */
@Composable
private fun ErrorMesage(isShown: Boolean) {
    if (isShown) {
        Text(
            text = "Enter a valid GitHub username.",
            modifier = Modifier
                .width(155.dp)
                .height(30.dp)
                .absoluteOffset(x = 15.dp, y = 44.dp),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

/**
 * Displays a button on the login form.
 *
 * @param enabled controls the enabled state of this button.
 *                If `false`, the button cannot be pressed.
 * @param onClick called when this button is clicked.
 */
@Composable
private fun LoginButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .width(180.dp)
            .height(40.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Text(
            text = "Login",
            style = MaterialTheme.typography.displayMedium
        )
    }
}

/**
 * Displays a login verification page.
 *
 * Displays the user code and provides instructions for completing the verification process.
 * Upon successful verification, the user will receive tokens and be redirected
 * to the `Mentions` page. If verification fails, the user will need to try again.
 *
 * If the user code expires before verification is complete, the process must be restarted.
 * In this case, the user code cannot be copied, and the instructions and button
 * will not be displayed.
 *
 * @param flow the control flow for the user login process.
 * @param toMentionsPage the navigation to the 'Mentions' page.
 */
@Composable
private fun VerificationPage(
    flow: LoginFlow,
    toMentionsPage: () -> Unit
) {
    val userCode by flow.userCode.collectAsState()
    val verificationUrl by flow.verificationUrl.collectAsState()
    val expiresIn by flow.expiresIn.collectAsState()
    val isUserCodeExpired by flow.isUserCodeExpired.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.secondary),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        VerificationTitle()
        Spacer(Modifier.height(15.dp))
        UserCodeField(
            userCode = userCode!!,
            isExpired = isUserCodeExpired
        )
        Spacer(Modifier.height(10.dp))
        if (isUserCodeExpired) {
            Spacer(Modifier.height(5.dp))
            CodeExpiredErrorMessage(flow)
        } else {
            VerificationText(
                verificationUrl = verificationUrl!!,
                expiresIn = expiresIn!!
            )
            Spacer(Modifier.height(20.dp))
            SubmitButton(
                flow = flow,
                toMentionsPage = toMentionsPage
            )
        }
    }
}

/**
 * Displays a title of the login verification page.
 */
@Composable
private fun VerificationTitle() {
    Text(
        text = "Verify your login",
        fontSize = 18.sp,
        style = MaterialTheme.typography.displayLarge
    )
}

/**
 * Displays the user code with an option to copy it.
 *
 * @param userCode the verification code to be displayed.
 * @param isExpired whether `userCode` is expired.
 */
@Composable
private fun UserCodeField(
    userCode: UserCode,
    isExpired: Boolean
) {
    val color = if (isExpired) {
        MaterialTheme.colorScheme.onBackground
    } else {
        MaterialTheme.colorScheme.onSecondary
    }
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        SelectionContainer {
            Text(
                text = userCode.value,
                color = color,
                fontSize = 28.sp,
                letterSpacing = 3.sp,
                style = MaterialTheme.typography.displayLarge
            )
        }
        if (!isExpired) {
            CopyToClipboardIcon(userCode)
        }
    }
}

/**
 * Displays an icon to copy the specified `UserCode` to the clipboard.
 *
 * @param userCode the `UserCode` to copy.
 */
@Composable
private fun CopyToClipboardIcon(
    userCode: UserCode
) {
    val clipboardManager = LocalClipboardManager.current
    Box(
        modifier = Modifier.offset(x = 100.dp)
    ) {
        IconButton(
            icon = Icons.copy,
            onClick = {
                clipboardManager.setText(AnnotatedString(userCode.value))
            },
            modifier = Modifier.size(30.dp),
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        )
    }
}

/**
 * Displays an error message indicating that the `UserCode` has expired.
 *
 * @param flow the control flow for the user login process.
 */
@Composable
private fun CodeExpiredErrorMessage(flow: LoginFlow) {
    ClickableErrorMessage(
        text = "The code has expired, please start over.",
        clickablePartOfText = "start over",
        onClick = {
            flow.requestUserCode(flow.username)
        }
    )
}

/**
 * Displays instructions for login verification.
 *
 * @param verificationUrl the URL of the GitHub verification page.
 * @param expiresIn the duration after which the `userCode` expires.
 */
@Composable
private fun VerificationText(
    verificationUrl: Url,
    expiresIn: Duration
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Enter this code at",
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(3.dp))
        VerificationUrlButton(verificationUrl)
        Spacer(Modifier.height(3.dp))
        Text(
            text = "The code is valid for ${toMinutes(expiresIn)} minutes.",
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

/**
 * Displays a URL of the GitHub verification page.
 *
 * @param url the URL of the GitHub verification page.
 */
@Composable
private fun VerificationUrlButton(url: Url) {
    val uriHandler = LocalUriHandler.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val decoration = if (isHovered) {
        TextDecoration.Underline
    } else {
        TextDecoration.None
    }
    Text(
        text = url.spec,
        modifier = Modifier
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                uriHandler.openUri(url.spec)
            },
        color = MaterialTheme.colorScheme.primary,
        textDecoration = decoration,
        style = MaterialTheme.typography.bodyLarge
    )
}

/**
 * Displays a button to confirm verification.
 *
 * @param flow the control flow for the user login process.
 * @param toMentionsPage the navigation to the 'Mentions' page.
 */
@Composable
private fun SubmitButton(
    flow: LoginFlow,
    toMentionsPage: () -> Unit
) {
    val enabled by flow.isAccessTokenRequestAvailable.collectAsState()
    val onClick = {
        flow.verify(
            onSuccess = {
                toMentionsPage()
            }
        )
    }
    Box(
        modifier = Modifier
            .width(210.dp)
            .height(32.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxSize(),
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                text = "I have entered the code",
                style = MaterialTheme.typography.displayMedium
            )
        }
        if (!enabled) {
            NoResponseErrorMessage(
                flow = flow
            )
        }
    }
}

/**
 * Displays an error message indicating that GitHub could not verify the login.
 *
 * @param flow the control flow for the user login process.
 */
@Composable
private fun NoResponseErrorMessage(
    flow: LoginFlow
) {
    val interval by flow.interval.collectAsState()
    ClickableErrorMessage(
        text = """
                No response from GitHub yet.
                Try again in ${interval!!.seconds} seconds, or start over.
            """.trimIndent(),
        clickablePartOfText = "start over",
        onClick = {
            flow.requestUserCode(flow.username)
        },
        modifier = Modifier
            .width(180.dp)
            .offset(y = 40.dp)
    )
}

/**
 * Displays an error message, part of the text of which is a clickable link.
 *
 * @param text the error text.
 * @param clickablePartOfText The substring of `text` that is a link.
 * @param onClick called when `clickablePartOfText` is clicked.
 * @param modifier the modifier to be applied to this error message.
 * @throws IllegalArgumentException if `clickablePartOfText` is not substring of the `text`.
 */
@Composable
private fun ClickableErrorMessage(
    text: String,
    clickablePartOfText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    require(text.contains(clickablePartOfText)) {
        "The `clickablePartOfText` must be a substring of the `text`."
    }
    val startPosition = text.indexOf(clickablePartOfText)
    val endPosition = startPosition + clickablePartOfText.length
    val annotatedString = buildAnnotatedString {
        append(text)
        addStringAnnotation(
            tag = "Action",
            annotation = clickablePartOfText,
            start = startPosition,
            end = endPosition
        )
        addStyle(
            style = SpanStyle(
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline
            ),
            start = startPosition,
            end = endPosition
        )
    }
    ClickableText(
        text = annotatedString,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
    ) { offset ->
        annotatedString
            .getStringAnnotations(offset, offset)
            .firstOrNull()?.let {
                onClick()
            }
    }
}
