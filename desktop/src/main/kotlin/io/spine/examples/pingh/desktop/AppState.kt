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
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import io.spine.examples.pingh.client.NotificationSender
import io.spine.examples.pingh.client.PinghApplication
import java.awt.Image
import java.awt.TrayIcon
import java.util.Properties

/**
 * Creates the state of the application.
 */
@Composable
internal fun createAppState(): AppState {
    val trayIcon = Icons.tray.toAwtImage(LocalDensity.current, LocalLayoutDirection.current)
    return AppState(trayIcon)
}

/**
 * The top-level application state.
 *
 * @param trayIcon The image to be displayed in the tray.
 */
internal class AppState(trayIcon: Image) {
    /**
     * State of the window.
     */
    internal val window = WindowState()

    /**
     * The application icon, located in the platform taskbar.
     *
     * Also, provides access to send notifications.
     */
    internal val tray = TrayIcon(trayIcon, window.title)

    /**
     * Manages the logic for the Pingh app.
     */
    internal val app: PinghApplication

    init {
        val notificationSender = TrayNotificationSender(tray) { !window.isShown }
        val properties = loadProperties()
        app = PinghApplication.builder()
            .withAddress(properties.getProperty("server.address"))
            .withPort(properties.getProperty("server.port").toInt())
            .with(notificationSender)
            .build()
    }

    /**
     * Loads server properties from resource folder.
     */
    private fun loadProperties(): Properties {
        val properties = Properties()
        val path = "/config/server.properties"
        javaClass.getResourceAsStream(path).use {
            properties.load(it)
        }
        check(properties.containsKey("server.address")) {
            "The Pingh server address must be provided in the configuration file " +
                    "located at \"resource$path\"."
        }
        check(properties.containsKey("server.port")) {
            "The Pingh server port must be provided in the configuration file " +
                    "located at \"resource$path\"."
        }
        return properties
    }

    /**
     * Switches the window visibility.
     */
    internal fun toggleWindowVisibility() {
        window.isShown = !window.isShown
    }
}

/**
 * Allows to send notifications to the system tray.
 *
 * Notifications will only be sent if the window is hidden but the application is run.
 *
 * @property tray The tray icon that enables sending notifications.
 * @property isWindowHidden Returns `true` if the [window][AppState.window] is hidden;
 *   returns `false` otherwise.
 */
private class TrayNotificationSender(
    private val tray: TrayIcon,
    private val isWindowHidden: () -> Boolean
) : NotificationSender {

    /**
     * Sends the information notification to the system tray.
     *
     * @param title The notification's title.
     * @param content The notification's content.
     */
    override fun send(title: String, content: String) {
        if (isWindowHidden()) {
            tray.displayMessage(title, content, TrayIcon.MessageType.INFO)
        }
    }
}
