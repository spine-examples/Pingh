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
import androidx.compose.ui.window.ApplicationScope
import io.spine.examples.pingh.client.PinghApplication
import java.awt.Image
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent

/**
 * Adds the application icon to the platform taskbar.
 *
 * This icon allows the user to show or hide the Pingh window and quit the application.
 *
 * @param state The state of the application icon, located in the platform taskbar.
 * @param app Manages the logic for the Pingh app.
 */
@Composable
internal fun ApplicationScope.Tray(state: TrayState, app: PinghApplication) {
    state.systemTray.apply {
        isImageAutoSize = true
        toolTip = state.title

        addMouseListener(
            object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    if (e.button == 1) {
                        state.toggleWindowVisibility()
                    }
                }
            }
        )
    }
    SystemTray.getSystemTray().add(state.systemTray)
}

/**
 * State of [Tray].
 *
 * @property window The state of the Pingh platform window.
 */
internal class TrayState(
    icon: Image,
    private val window: WindowState
) {
    // TODO:2024-10-31:mykyta.pimonov: Rename.
    internal val systemTray = TrayIcon(icon)

    /**
     * Application's title.
     */
    internal val title: String
        get() = window.title

    /**
     * Switches the window visibility.
     */
    internal fun toggleWindowVisibility() {
        window.isShown = !window.isShown
    }
}
