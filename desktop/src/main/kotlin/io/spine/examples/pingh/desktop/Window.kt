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

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.Window as ComposeWindow
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import java.awt.Desktop
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.SwingUtilities

/**
 * Displays Pingh platform window.
 *
 * @param state The top-level application state.
 * @see [PlatformWindow]
 */
@Composable
internal fun Window(state: AppState) {
    PlatformWindow(
        state = state.window,
        onClose = state.window::hide
    ) {
        WindowContent(state)
    }
}

/**
 * Displays a transparent, undecorated platform window.
 *
 * Ideally, this window would align with the tray icon's position. However,
 * due to the current lack of API support in Compose, the window is placed
 * near the general area of the tray icons.
 *
 * If a click outside the window occurs, the window is hidden.
 *
 * @see <a href="https://github.com/JetBrains/compose-multiplatform/issues/289">Issue #289</a>
 */
@Composable
private fun PlatformWindow(
    state: WindowState,
    onClose: () -> Unit,
    content: @Composable FrameWindowScope.() -> Unit
) {
    val windowState = rememberWindowState(
        width = 460.dp,
        height = 740.dp,
        position = WindowPosition(1200.dp, 30.dp)
    )
    ComposeWindow(
        onCloseRequest = onClose,
        state = windowState,
        visible = state.isShown,
        title = state.title,
        undecorated = true,
        transparent = true,
        resizable = false,
        focusable = true
    ) {
        content()

        DisposableEffect(window) {
            // Focuses on the window and brings it to the front when displayed.
            val listener = object : WindowAdapter() {
                override fun windowActivated(e: WindowEvent?) {
                    SwingUtilities.invokeLater {
                        window.requestFocus()
                    }
                }
            }
            window.addWindowListener(listener)

            // Hides the window when it loses focus.
            val focusListener = object : WindowAdapter() {
                override fun windowLostFocus(e: WindowEvent?) {
                    state.isShown = false
                }
            }
            window.addWindowFocusListener(focusListener)

            onDispose {
                window.removeWindowListener(listener)
                window.removeWindowFocusListener(focusListener)
            }
        }
    }

    LaunchedEffect(state.isShown) {
        if (state.isShown) {
            Desktop.getDesktop().requestForeground(true)
        }
    }
}

/**
 * Displays a content in the Pingh window.
 */
@Composable
private fun WindowContent(state: AppState) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .width(420.dp)
                .height(700.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onBackground,
                    shape = MaterialTheme.shapes.small
                )
                .shadow(
                    elevation = 10.dp,
                    shape = MaterialTheme.shapes.small
                )
                .clip(MaterialTheme.shapes.small)
        ) {
            CurrentPage(state.app, state::closeAndExit)
        }
    }
}

/**
 * State of [Window].
 */
internal class WindowState {
    /**
     * Window's title.
     */
    internal val title = "Pingh"

    /**
     * Whether window is shown.
     */
    internal var isShown by mutableStateOf(true)

    /**
     * Sets the window visibility to `false`.
     */
    internal fun hide() {
        isShown = false
    }
}
