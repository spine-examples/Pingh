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

import com.google.protobuf.Timestamp
import io.spine.base.EventMessage
import io.spine.base.EventMessageField
import io.spine.base.Field
import io.spine.client.EventFilter.eq
import io.spine.examples.pingh.client.settings.isIgnored
import io.spine.examples.pingh.github.Repo
import io.spine.examples.pingh.github.Team
import io.spine.examples.pingh.github.User
import io.spine.examples.pingh.github.Username
import io.spine.examples.pingh.github.tag
import io.spine.examples.pingh.mentions.event.MentionUnsnoozed
import io.spine.examples.pingh.mentions.event.UserMentioned
import io.spine.logging.Logging
import kotlin.reflect.KClass

/**
 * Allows to send notifications.
 */
public interface NotificationSender {

    /**
     * Creates and sends a notification with the given title and content.
     *
     * @param title The notification's title.
     * @param content The notification's content.
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
 * @property sender Allows to send notifications.
 * @property settings Manages the application settings configured by a user.
 */
internal class NotificationsFlow(
    private val sender: NotificationSender,
    private val settings: Settings
) : Logging {
    companion object {
        /**
         * List of information about available notifications.
         */
        private val notifications = listOf(
            NotificationInfo(
                UserMentioned::class,
                "Pingh",
                whereMentioned = { it.whereMentioned },
                content = { event ->
                    content(
                        event.whenMentioned,
                        event.whoMentioned,
                        event.title,
                        if (event.hasViaTeam()) event.viaTeam else null
                    )
                }
            ),
            NotificationInfo(
                MentionUnsnoozed::class,
                "Pingh",
                whereMentioned = { it.whereMentioned },
                content = { event ->
                    content(
                        event.whenMentioned,
                        event.whoMentioned,
                        event.title,
                        if (event.hasViaTeam()) event.viaTeam else null
                    )
                }
            )
        )

        private fun content(
            whenMentioned: Timestamp,
            whoMentioned: User,
            title: String,
            viaTeam: Team? = null
        ): String {
            val teamDetails = if (viaTeam != null) {
                " via ${viaTeam.tag}"
            } else ""
            return "${whenMentioned.howMuchTimeHasPassed().uppercase()} " +
                    "${whoMentioned.username.value} mentioned you$teamDetails in '${title}'."
        }
    }

    /**
     * Sends a notification with the specified [title] and [content]
     * if 'Do Not Disturb' mode is disabled.
     */
    internal fun send(title: String, content: String) {
        if (!settings.current.dndEnabled) {
            sender.send(title, content)
            _debug().log(
                "A notification was sent with the title \"$title\" " +
                        "and the content \"$content\"."
            )
        }
    }

    /**
     * Enables the sending of notifications for the provided client with the given username.
     *
     * @param client Enables subscription to events emitted by the Pingh server.
     * @param username The username for which mentions will be sent.
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
        ) { event ->
            if (settings.current.run {
                    !dndEnabled && !isIgnored(notification.whereMentioned(event))
                }
            ) {
                val content = notification.content(event)
                sender.send(notification.title, content)
                _debug().log(
                    "A notification was sent with the title \"${notification.title}\" " +
                            "and the content \"$content\"."
                )
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
     * @param E The type of the event that triggers the sending of a notification.
     *
     * @property onEvent The event that triggers the sending of a notification.
     * @property title The notification's title.
     * @property whereMentioned Returns the repository where the user was mentioned.
     * @property content The notification's content.
     */
    private data class NotificationInfo<E : EventMessage>(
        val onEvent: KClass<E>,
        val title: String,
        val whereMentioned: (event: E) -> Repo,
        val content: (event: E) -> String
    )
}
