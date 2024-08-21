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
import androidx.compose.ui.window.TrayState
import io.spine.examples.pingh.client.NotificationSender
import io.spine.examples.pingh.client.PinghApplication

/**
 * The top-level application state.
 *
 * @param isSystemInDarkTheme whether current system theme is set to 'Dark'.
 */
internal class PinghApplicationState(isSystemInDarkTheme: Boolean) {

    /**
     * A built-in state for Compose trays.
     *
     * Enables sending notifications.
     */
    private val composeTray = TrayState()

    /**
     * State of the window.
     */
    internal val window = PinghWindowState()

    /**
     * State of the application icon, located in the platform taskbar.
     */
    internal val tray = PinghTrayState(window, composeTray, isSystemInDarkTheme)

    /**
     * Manages the logic for the Pingh app.
     */
    internal val app: PinghApplication

    init {
        val notificationSender = TrayNotificationSender(composeTray) { !window.isShown }
        app = PinghApplication(notificationSender)
    }
}

/**
 * Allows you to send notifications to the system tray.
 *
 * Notifications will only be sent if the window is hidden but the application is enabled.
 *
 * @param composeTray the built-in state for Compose trays.
 * @param isWindowHidden returns `true` if the [window][PinghApplicationState.window] is hidden;
 *                       returns `false` otherwise.
 */
private class TrayNotificationSender(
    private val composeTray: TrayState,
    private val isWindowHidden: () -> Boolean
) : NotificationSender {

    /**
     * Sends the information [Notification] to the system tray.
     *
     * @param title the notification's title.
     * @param content the notification's content.
     */
    override fun send(title: String, content: String) {
        if (isWindowHidden()) {
            val notification = Notification(title, content, Notification.Type.Info)
            composeTray.sendNotification(notification)
        }
    }
}
