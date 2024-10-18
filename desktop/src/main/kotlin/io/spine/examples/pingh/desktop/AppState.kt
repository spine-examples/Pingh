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

import androidx.compose.ui.window.Notification
import androidx.compose.ui.window.TrayState as ComposeTrayState
import io.spine.examples.pingh.client.NotificationSender
import io.spine.examples.pingh.client.PinghApplication
import java.util.Properties

/**
 * The top-level application state.
 *
 * @param settings The settings of the operating system on which the application is running.
 */
internal class AppState(settings: SystemSettings) {

    /**
     * A built-in state for Compose trays.
     *
     * Enables sending notifications.
     */
    private val composeTray = ComposeTrayState()

    /**
     * State of the window.
     */
    internal val window = WindowState()

    /**
     * State of the application icon, located in the platform taskbar.
     */
    internal val tray = TrayState(window, composeTray, settings)

    /**
     * Manages the logic for the Pingh app.
     */
    internal val app: PinghApplication

    init {
        val notificationSender = TrayNotificationSender(composeTray) { !window.isShown }
        val properties = loadServerProperties()
        app = PinghApplication.builder()
            .withAddress(properties.getProperty("server.address"))
            .withPort(properties.getProperty("server.port").toInt())
            .with(notificationSender)
            .build()
    }
}

/**
 * Allows to send notifications to the system tray.
 *
 * Notifications will only be sent if the window is hidden but the application is run.
 *
 * @property composeTray The built-in state for Compose trays.
 * @property isWindowHidden Returns `true` if the [window][AppState.window] is hidden;
 *   returns `false` otherwise.
 */
private class TrayNotificationSender(
    private val composeTray: ComposeTrayState,
    private val isWindowHidden: () -> Boolean
) : NotificationSender {

    /**
     * Sends the information [Notification] to the system tray.
     *
     * @param title The notification's title.
     * @param content The notification's content.
     */
    override fun send(title: String, content: String) {
        if (isWindowHidden()) {
            val notification = Notification(title, content, Notification.Type.Info)
            composeTray.sendNotification(notification)
        }
    }
}

/**
 * Loads server properties from resource folder.
 */
internal fun loadServerProperties(): Properties {
    val properties = Properties()
    val path = "/config/server.properties"
    AppState::class.java.getResourceAsStream(path).use {
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
