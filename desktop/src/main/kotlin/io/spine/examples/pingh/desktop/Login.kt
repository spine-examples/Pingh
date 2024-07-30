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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.UrlAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.protobuf.Duration
import io.spine.examples.pingh.client.DesktopClient
import io.spine.examples.pingh.github.UserCode
import io.spine.examples.pingh.github.Username
import io.spine.examples.pingh.github.buildBy
import io.spine.examples.pingh.github.validateUsernameValue
import io.spine.net.Url
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Displays the page with the current login step.
 *
 * Initially, the user must enter their `Username`, after which they will receive
 * a code that must be entered into GitHub. After entering the code, the user needs
 * to confirm the login on the application page.
 */
@Composable
internal fun LoginPage(
    client: DesktopClient,
    toMentionsPage: () -> Unit
) {
    var state by remember { mutableStateOf(LoginState.USERNAME_ENTERING) }
    var verificationInfo by remember { mutableStateOf<VerificationInfo?>(null) }
    when (state) {
        LoginState.USERNAME_ENTERING -> UsernameEnteringPage(
            client = client
        ) { userCode, verificationUrl, interval ->
            verificationInfo = VerificationInfo(userCode, verificationUrl, interval)
            state = LoginState.VERIFICATION
        }

        LoginState.VERIFICATION -> VerificationPage(
            client = client,
            verificationInfo = verificationInfo!!,
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
 * @param client enables interaction with the Pingh server.
 * @param toVerifyingPage the navigation to the 'Login verification' page.
 */
@Composable
private fun UsernameEnteringPage(
    client: DesktopClient,
    toVerifyingPage: (userCode: UserCode, verificationUrl: Url, interval: Duration) -> Unit
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
            client.logIn(
                Username::class.buildBy(username)
            ) { event ->
                toVerifyingPage(event.userCode, event.verificationUrl, event.interval)
            }
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
        else -> MaterialTheme.colorScheme.tertiary
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

@Composable
private fun VerificationPage(
    client: DesktopClient,
    verificationInfo: VerificationInfo,
    toMentionsPage: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.secondary),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        VerificationText(verificationInfo.verificationUrl)
        Spacer(Modifier.height(10.dp))
        UserCodeField(verificationInfo.userCode)
        Spacer(Modifier.height(10.dp))
        VerificationButton(client, verificationInfo.interval, toMentionsPage)
    }
}

@Composable
@OptIn(ExperimentalTextApi::class) // Required for `UrlAnnotation`.
private fun VerificationText(
    verificationUrl: Url
) {
    val urlHandler = LocalUriHandler.current
    val text = "Go to GitHub and enter this code there."
    val startPosition = text.indexOf("GitHub")
    val endPosition = startPosition + "GitHub".length
    val annotatedString = buildAnnotatedString {
        append(text)
        addStyle(
            style = SpanStyle(
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline
            ),
            start = startPosition,
            end = endPosition
        )
        addUrlAnnotation(
            urlAnnotation = UrlAnnotation(verificationUrl.spec),
            start = startPosition,
            end = endPosition
        )
    }
    ClickableText(
        text = annotatedString,
        style = MaterialTheme.typography.bodyLarge,
        onClick = { offset ->
            annotatedString
                .getUrlAnnotations(offset, offset)
                .firstOrNull()?.let { annotation ->
                    urlHandler.openUri(annotation.item.url)
                }
        }
    )
}

@Composable
private fun UserCodeField(
    userCode: UserCode
) {
    val clipboardManager = LocalClipboardManager.current
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .pointerHoverIcon(PointerIcon.Hand)
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                clipboardManager.setText(AnnotatedString(userCode.value))
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally)
    ) {
        userCode.value.forEach { lexeme ->
            if (lexeme == '-') {
                UsecCodeChar(lexeme)
            } else {
                Box(
                    modifier = Modifier
                        .width(22.dp)
                        .height(30.dp)
                        .border(
                            color = MaterialTheme.colorScheme.onBackground,
                            width = 1.dp,
                            shape = MaterialTheme.shapes.extraSmall
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    UsecCodeChar(lexeme)
                }
            }
        }
    }
}

@Composable
private fun UsecCodeChar(lexeme: Char) {
    Text(
        text = lexeme.toString(),
        color = MaterialTheme.colorScheme.onSecondary,
        fontSize = 18.sp,
        style = MaterialTheme.typography.displayLarge
    )
}

@Composable
private fun VerificationButton(
    client: DesktopClient,
    interval: Duration,
    toMentionsPage: () -> Unit
) {
    val enabled = remember { mutableStateOf(true) }
    Button(
        onClick = {
            client.verifyLoginToGitHub(
                onSuccess = {
                    toMentionsPage()
                },
                onFail = {
                    enabled.value = false
                    makeButtonEnable(interval, enabled)
                }
            )
        },
        modifier = Modifier
            .width(210.dp)
            .height(32.dp),
        enabled = enabled.value,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Text(
            text = "Verify login",
            style = MaterialTheme.typography.displayMedium
        )
    }
}

private fun makeButtonEnable(
    interval: Duration,
    enabled: MutableState<Boolean>
) =
    CoroutineScope(Dispatchers.IO).launch {
        Thread.sleep(interval.seconds * 1000)
        enabled.value = true
    }

/**
 * Information required to verify login .
 *
 * @param userCode the verification code that displays so that
 *                 the user can enter the code in a browser.
 * @param verificationUrl the URL where users need to enter their `userCode`.
 * @param interval the minimum duration that must pass before user can make
 *                 a new access token request.
 */
private class VerificationInfo(
    internal val userCode: UserCode,
    internal val verificationUrl: Url,
    internal val interval: Duration
)

/**
 * State of login process.
 */
private enum class LoginState {

    /**
     * Initial state where the user enters their `Username` and receives a `UserCode`.
     */
    USERNAME_ENTERING,

    /**
     * The final step where the user enters their `UserCode` into GitHub and
     * completes the login process in the Pingh app.
     */
    VERIFICATION
}
