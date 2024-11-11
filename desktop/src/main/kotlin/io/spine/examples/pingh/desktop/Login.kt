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

@file:Suppress("TooManyFunctions" /* Using Compose requires many functions to render the UI. */)

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.protobuf.Duration
import io.spine.example.pingh.desktop.generated.resources.Res
import io.spine.example.pingh.desktop.generated.resources.copy
import io.spine.example.pingh.desktop.generated.resources.pingh
import io.spine.examples.pingh.client.LoginFlow
import io.spine.examples.pingh.client.EnterUsername
import io.spine.examples.pingh.client.LoginFailed
import io.spine.examples.pingh.client.VerifyLogin
import io.spine.examples.pingh.github.UserCode
import io.spine.examples.pingh.github.Username
import io.spine.examples.pingh.github.of
import io.spine.examples.pingh.github.isValidUsername
import io.spine.net.Url
import org.jetbrains.compose.resources.painterResource

/**
 * Displays the page with the current login stage.
 *
 * Initially, the user must enter their `Username`, after which they will receive
 * a code that must be entered into GitHub. After entering the code, the user needs
 * to confirm the login on the application page.
 *
 * @param flow The control flow of the GitHub login process.
 * @param toMentionsPage The navigation to the 'Mentions' page.
 */
@Composable
internal fun LoginPage(
    flow: LoginFlow,
    toMentionsPage: () -> Unit
) {
    val stage by flow.currentStage().collectAsState()
    when (val screenStage = stage) {
        is EnterUsername -> UsernameEnteringPage(
            flow = screenStage
        )

        is VerifyLogin -> VerificationPage(
            flow = screenStage,
            toMentionsPage = toMentionsPage
        )

        is LoginFailed -> FailedPage(
            flow = screenStage
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
 * @param flow The control flow of the GitHub login process stage
 *   where the user must enter their name.
 */
@Composable
private fun UsernameEnteringPage(
    flow: EnterUsername
) {
    var username by remember { mutableStateOf("") }
    var wasChanged by remember { mutableStateOf(false) }
    val isError = remember { mutableStateOf(false) }
    val requestUserCode = {
        val name = Username::class.of(username)
        flow.requestUserCode(name)
    }
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
            onEnterPressed = {
                if (wasChanged && !isError.value) {
                    requestUserCode()
                }
            },
            isError = isError
        )
        Spacer(Modifier.height(15.dp))
        LoginButton(
            enabled = wasChanged && !isError.value,
            onClick = requestUserCode
        )
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
                painter = painterResource(Res.drawable.pingh),
                contentDescription = null,
                modifier = Modifier.size(50.dp),
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
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Pingh is a GitHub app that looks up mentions on behalf of the user. " +
                    "It requires authentication via GitHub.",
            modifier = Modifier.width(240.dp),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * Displays a username input field on the login form.
 *
 * @param value The current value of the input field.
 * @param onChange Called when input value is changed.
 * @param onEnterPressed Called when this input is focused and the "Enter" key is pressed.
 * @param isError Indicates if the input's current value is in error.
 */
@Composable
private fun UsernameInput(
    value: String,
    onChange: (String) -> Unit,
    onEnterPressed: () -> Unit,
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
            isError.value = !isValidUsername(changedValue)
        },
        modifier = Modifier
            .width(240.dp)
            .height(57.dp)
            .onKeyEvent { event ->
                if (event.key == Key.Enter) {
                    onEnterPressed()
                    true
                } else {
                    false
                }
            }
            .testTag("username-input"),
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
            ErrorMessage(isError.value)
        }
    }
}

/**
 * Displays a container for the text field.
 *
 * @param value The current value of the input field.
 * @param textField The composable function that displays the content of an input field.
 * @param border The border of this input.
 */
@Composable
private fun InputContainer(
    value: String,
    textField: @Composable () -> Unit,
    border: BorderStroke
) {
    Row(
        modifier = Modifier
            .width(240.dp)
            .height(45.dp)
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
 * @param color The color of this label.
 */
@Composable
private fun Label(color: Color) {
    Box(
        modifier = Modifier
            .width(110.dp)
            .height(12.dp)
            .absoluteOffset(x = 12.dp, y = (-6).dp)
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
 * @param isShown Indicates whether the placeholder is displayed.
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
 * @param isShown Indicates whether the error message is displayed.
 */
@Composable
private fun ErrorMessage(isShown: Boolean) {
    if (isShown) {
        Text(
            text = "Enter a valid GitHub username.",
            modifier = Modifier
                .width(155.dp)
                .height(30.dp)
                .absoluteOffset(x = 15.dp, y = 49.dp),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

/**
 * Displays a button on the login form.
 *
 * @param enabled Controls the enabled state of this button.
 *   If `false`, the button cannot be pressed.
 * @param onClick Called when this button is clicked.
 */
@Composable
private fun LoginButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .width(240.dp)
            .height(45.dp)
            .testTag("login-button"),
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
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
 * @param flow The control flow of the GitHub login process stage
 *   where the user must verify their login.
 * @param toMentionsPage The navigation to the 'Mentions' page.
 */
@Composable
private fun VerificationPage(
    flow: VerifyLogin,
    toMentionsPage: () -> Unit
) {
    flow.waitForAuthCompletion(onSuccess = toMentionsPage)
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
        Spacer(Modifier.height(25.dp))
        UserCodeField(
            userCode = userCode,
            isExpired = isUserCodeExpired
        )
        Spacer(Modifier.height(25.dp))
        if (isUserCodeExpired) {
            Spacer(Modifier.height(5.dp))
            CodeExpiredErrorMessage(flow)
        } else {
            VerifyLoginSection(
                verificationUrl = verificationUrl,
                expiresIn = expiresIn
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
        fontSize = 20.sp,
        style = MaterialTheme.typography.displayLarge
    )
}

/**
 * Displays the user code with an option to copy it.
 *
 * @param userCode The verification code to be displayed.
 * @param isExpired Whether `userCode` is expired.
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
    Row(
        modifier = Modifier
            .width(280.dp)
            .height(46.dp)
            .run {
                if (isExpired) {
                    this
                } else {
                    border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.onBackground,
                        shape = MaterialTheme.shapes.medium
                    )
                }
            }
            .padding(horizontal = 15.dp)
            .testTag("user-code"),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SelectionContainer {
            Text(
                text = userCode.value,
                modifier = Modifier.width(220.dp),
                color = color,
                fontSize = 30.sp,
                textAlign = if (isExpired) TextAlign.Center else TextAlign.Start,
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
 * @param userCode The `UserCode` to copy.
 */
@Composable
private fun CopyToClipboardIcon(
    userCode: UserCode
) {
    val clipboardManager = LocalClipboardManager.current
    IconButton(
        icon = painterResource(Res.drawable.copy),
        onClick = {
            clipboardManager.setText(AnnotatedString(userCode.value))
        },
        modifier = Modifier.size(30.dp),
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    )
}

/**
 * Displays an error message indicating that the `UserCode` has expired.
 *
 * @param flow The control flow of the GitHub login process stage
 *   where the user must verify their login.
 */
@Composable
private fun CodeExpiredErrorMessage(flow: VerifyLogin) {
    ClickableErrorMessage(
        text = "The code has expired, please start over.",
        clickablePartOfText = "start over",
        onClick = flow::requestNewUserCode
    )
}

/**
 * Displays the remaining time for verification and instructions for confirming login.
 *
 * @param verificationUrl The URL of the GitHub verification page.
 * @param expiresIn The duration after which the `userCode` expires.
 */
@Composable
private fun VerifyLoginSection(
    verificationUrl: Url,
    expiresIn: Duration
) {
    Row(
        modifier = Modifier.width(280.dp).height(55.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        CountdownTimer(
            minutes = expiresIn.minutesOfHour,
            seconds = expiresIn.secondsOfMinute,
            size = 55.dp,
            indicatorColor = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.background
        )
        VerificationText(verificationUrl)
    }
}

/**
 * Displays instructions for login verification.
 *
 * @param url The URL of the GitHub verification page.
 */
// TODO:2024-11-06:mykyta.pimonov: Rewrite using an `AnnotatedString` with a `LinkAnnotation`
//  when upgrading to Compose Multiplatform 1.7.0.
@Composable
private fun VerificationText(url: Url) {
    val uriHandler = LocalUriHandler.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val decoration = if (isHovered) {
        TextDecoration.Underline
    } else {
        TextDecoration.None
    }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Enter this code at",
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(5.dp))
        Row {
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
            Text(".")
        }
    }
}

/**
 * Displays an error message, part of the text of which is a clickable link.
 *
 * @param text The error text.
 * @param clickablePartOfText The substring of `text` that is a link.
 * @param onClick Called when `clickablePartOfText` is clicked.
 * @param modifier The modifier to be applied to this error message.
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
    val index = text.indexOf(clickablePartOfText)
    val annotatedString = buildAnnotatedString {
        append(text.substring(0, index))
        withLink(
            LinkAnnotation.Clickable(
                tag = "Action",
                styles = TextLinkStyles(
                    style = SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.None
                    ),
                    hoveredStyle = SpanStyle(
                        textDecoration = TextDecoration.Underline
                    )
                ),
                linkInteractionListener = { onClick() }
            )
        ) {
            append(clickablePartOfText)
        }
        append(text.substring(index + clickablePartOfText.length, text.length))
    }
    Text(
        text = annotatedString,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
    )
}

/**
 * Displays a login failed page.
 *
 * Displays the reason for the failed login and provides an option to restart the process.
 *
 * @param flow The stage in the GitHub login process control flow where the login attempt fails.
 */
@Composable
private fun FailedPage(flow: LoginFailed) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.secondary),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = flow.errorMessage.value,
            modifier = Modifier.width(240.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(25.dp))
        RestartButton(flow)
    }
}

/**
 * Displays a button to restart the login process.
 *
 * @param flow The stage in the GitHub login process control flow where the login attempt fails.
 */
@Composable
private fun RestartButton(flow: LoginFailed) {
    Button(
        onClick = flow::restartLogin,
        modifier = Modifier
            .width(240.dp)
            .height(40.dp),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Text(
            text = "Try to log in again",
            style = MaterialTheme.typography.displayMedium
        )
    }
}
