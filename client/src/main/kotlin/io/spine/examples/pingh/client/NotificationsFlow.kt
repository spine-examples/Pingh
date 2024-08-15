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

package io.spine.examples.pingh.client

import io.spine.base.EventMessage
import io.spine.base.EventMessageField
import io.spine.base.Field
import io.spine.client.EventFilter.eq
import io.spine.examples.pingh.github.Username
import io.spine.examples.pingh.mentions.event.MentionUnsnoozed
import io.spine.examples.pingh.mentions.event.UserMentioned
import kotlin.reflect.KClass

/**
 * Allows to send notifications.
 */
public interface NotificationSender {

    /**
     * Creates and sends a notification with the given header and text.
     *
     * @param title the notification's title.
     * @param content the notification's content.
     */
    public fun send(title: String, content: String)
}

/**
 * The flow that manages the sending of notifications within the app.
 *
 * In the Pingh application, notifications are triggered by predefined events.
 * When such an event is emitted, a notification is sent to the user.
 *
 * Notifications will be suppressed if 'Do Not Disturb' mode is enabled
 * in the application settings.
 *
 * Please note that notifications flow automatically subscribes to the necessary events
 * to generate notifications, but does not provide options to manage or close these subscriptions.
 * Ensure the previous [client][DesktopClient] is properly [closed][DesktopClient.close]
 * before initiating notifications for a new `client`.
 *
 * @param sender allows to send notifications.
 * @param settings the state of application settings.
 */
internal class NotificationsFlow(
    private val sender: NotificationSender,
    private val settings: SettingsState
) {
    companion object {
        /**
         * List of information about available notifications.
         */
        private val notifications = listOf(
            NotificationInfo(UserMentioned::class, "Pingh", "New mentions!"),
            NotificationInfo(MentionUnsnoozed::class, "Pingh", "Mentions unsnoozed!")
        )
    }

    /**
     * Enables the sending of notifications for the provided client with the given username.
     *
     * @param client enables subscription to events emitted by the Pingh server.
     * @param username the username for which mentions will be sent.
     */
    internal fun enableNotifications(client: DesktopClient, username: Username) {
        notifications.forEach { notification ->
            enable(client, username, notification)
        }
    }

    /**
     * Creates a subscription to an event that triggers the sending of
     * a notification when emitted.
     */
    private fun <E : EventMessage> enable(
        client: DesktopClient,
        username: Username,
        notification: NotificationInfo<E>
    ) {
        client.observeEvent(
            notification.onEvent,
            eq(usernameField(), username)
        ) {
            if (!settings.enabledDndMode.value) {
                sender.send(notification.title, notification.content)
            }
        }
    }

    /**
     * Returns the subscribable field of the username extracted from the mention ID.
     */
    private fun usernameField(): EventMessageField =
        EventMessageField(Field.named("id").nested("user"))

    /**
     * Information about available notification.
     *
     * @param onEvent the event that triggers the sending of a notification.
     * @param title the notification's title.
     * @param content the notification's content.
     */
    private data class NotificationInfo<E : EventMessage>(
        val onEvent: KClass<E>,
        val title: String,
        val content: String
    )
}