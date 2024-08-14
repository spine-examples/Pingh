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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import io.spine.examples.pingh.client.PinghApplication

/**
 * Displays Pingh platform window.
 *
 * @see [PlatformWindow]
 */
@Composable
internal fun PinghWindow(state: PinghWindowState) {
    PlatformWindow(
        title = state.title,
        isShown = state.isShown,
        onClose = state::hide
    ) {
        CurrentPage(state.app)
    }
}

/**
 * Displays a transparent, undecorated platform window.
 *
 * Ideally, this window would align with the tray icon's position. However,
 * due to the current lack of API support in Compose, the window is placed
 * near the general area of the tray icons.
 *
 * @see <a href="https://github.com/JetBrains/compose-multiplatform/issues/289">Issue #289</a>
 */
@Composable
private fun PlatformWindow(
    title: String,
    isShown: Boolean,
    onClose: () -> Unit,
    content: @Composable FrameWindowScope.() -> Unit
) {
    val windowState = rememberWindowState(
        width = 240.dp,
        height = 426.dp,
        position = WindowPosition(1100.dp, 30.dp)
    )
    Window(
        onCloseRequest = onClose,
        state = windowState,
        visible = isShown,
        title = title,
        undecorated = true,
        transparent = true,
        resizable = false,
        content = content
    )
}

/**
 * State of [PinghWindow].
 */
internal class PinghWindowState {

    /**
     * Manages the logic for the Pingh app.
     */
    internal val app = PinghApplication()

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
