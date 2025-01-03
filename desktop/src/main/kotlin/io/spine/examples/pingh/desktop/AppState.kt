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
 * @param serverEndpoint The connection details for the Pingh server.
 */
internal class AppState(serverEndpoint: ServerEndpoint) {
    /**
     * State of the window.
     */
    internal val window = WindowState()

    /**
     * A tray state that sends notifications
     * when configured within the [Tray] composable element.
     *
     * If the tray state is not configured when the notification is sent,
     * the notification will be ignored.
     */
    internal val tray = TrayState()

    /**
     * Manages the logic for the Pingh app.
     */
    internal val app: PinghApplication

    init {
        val notificationSender = TrayNotificationSender(tray) { !window.isShown }
        app = PinghApplication.builder()
            .withAddress(serverEndpoint.address)
            .withPort(serverEndpoint.port)
            .with(notificationSender)
            .build()
    }

    /**
     * Actions that are performed when the application is [closed][close].
     */
    internal val closureActions = mutableListOf<() -> Unit>()

    init {
        addClosureAction(app::close)
    }

    /**
     * Switches the window visibility.
     */
    internal fun toggleWindowVisibility() {
        window.isShown = !window.isShown
    }

    /**
     * Adds an action to be executed upon application closure,
     * following the order in which actions were added.
     */
    internal fun addClosureAction(onClose: () -> Unit) {
        closureActions.add(onClose)
    }

    /**
     * Closes the application.
     *
     * When the application is closed, all actions added
     * using [addClosureAction()][addClosureAction] method are executed sequentially.
     *
     * By default, the client connection to
     * the Pingh server is [closed][PinghApplication.close] first.
     */
    internal fun close() {
        closureActions.forEach { it() }
    }
}

/**
 * Allows to send notifications to the system tray.
 *
 * Notifications will only be sent if the window is hidden but the application is run.
 *
 * @property tray The tray state that sends notifications.
 * @property isWindowHidden Returns `true` if the [window][AppState.window] is hidden;
 *   returns `false` otherwise.
 */
private class TrayNotificationSender(
    private val tray: TrayState,
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
            tray.sendNotification(notification)
        }
    }
}
