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
import java.awt.Frame
import java.awt.MenuItem
import java.awt.PopupMenu
import java.awt.SystemTray
import java.awt.Window
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent

/**
 * Adds the application icon to the platform taskbar.
 *
 * Left-clicking the tray icon toggles the window’s visibility,
 * hiding it if open and displaying it if hidden.
 *
 * Right-clicking the tray icon opens the application's control menu.
 *
 * Because Java AWT lacks an API to obtain exact tray icon coordinates,
 * the menu location is dynamically calculated based on the click position.
 * As a result, the menu may appear in slightly different locations depending on where
 * the tray icon is clicked.
 *
 * @param state The top-level application state.
 * @throws IllegalStateException if the system tray is not supported on the current platform.
 */
@Composable
internal fun ApplicationScope.Tray(state: AppState) {
    if (!SystemTray.isSupported()) {
        throw IllegalStateException("The platform does not support tray applications.")
    }
    val menu = Menu {
        SystemTray.getSystemTray().remove(state.tray)
        state.app.close()
        exitApplication()
    }
    state.tray.apply {
        isImageAutoSize = true

        addMouseListener(
            object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    if (e.button == 1) {
                        state.toggleWindowVisibility()
                    }
                    if (e.button == 3) {
                        menu.show(e.xOnScreen, e.yOnScreen)
                    }
                }
            }
        )
    }
    SystemTray.getSystemTray().add(state.tray)
}

/**
 * The tray application menu provides controls such as an exit option for the application.
 *
 * When the menu is open, clicking anywhere outside of it will close the menu.
 *
 * @param onExit Called when the “Exit” button is pressed.
 */
private class Menu(onExit: () -> Unit) {
    /**
     * Utility window on which the application menu will be displayed.
     */
    private val frame = Frame()

    /**
     * The application menu includes an "Exit" button to close the application.
     */
    private val popup = PopupMenu()

    init {
        val exitItem = MenuItem("Exit")
        exitItem.addActionListener {
            onExit()
        }
        popup.add(exitItem)
        frame.apply {
            isUndecorated = true
            type = Window.Type.UTILITY
            add(popup)
            isVisible = true
        }
    }

    /**
     * Displays the popup menu at the specified (`x`, `y`) position relative to the full screen.
     */
    fun show(x: Int, y: Int) {
        popup.show(frame, x, y)
    }
}
