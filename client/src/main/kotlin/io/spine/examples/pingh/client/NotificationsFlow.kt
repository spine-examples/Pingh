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
import io.spine.examples.pingh.github.Username
import io.spine.examples.pingh.mentions.event.MentionUnsnoozed
import io.spine.examples.pingh.mentions.event.UserMentioned
import kotlin.reflect.KClass

/**
 * Allows to send notifications to user.
 */
public interface UserAlert {

    /**
     * Creates and sends a session expiration notification.
     */
    public fun notifySessionExpired()

    /**
     * Creates and sends a mention notification.
     */
    public fun notifyMention(mention: MentionDetails)
}

/**
 * Information about a mention.
 *
 * @property title The title of the GitHub page in which the mention created.
 * @property whenMentioned Time when the user was mentioned.
 * @property whoMentioned The user who created the mention.
 * @property whereMentioned The repository where the user was mentioned.
 * @property viaTeam The team through which the user was mentioned due to their membership.
 *   If the user was mentioned by username, this field is `null`.
 */
public data class MentionDetails(
    public val title: String,
    public val whenMentioned: Timestamp,
    public val whoMentioned: Username,
    public val whereMentioned: Repo,
    public val viaTeam: Team? = null
)

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
 * @property alert Allows to send notifications to user.
 * @property settings Manages the application settings configured by a user.
 */
internal class NotificationsFlow(
    private val alert: UserAlert,
    private val settings: Settings
) {
    companion object {
        /**
         * List of information about available mention notifications.
         */
        private val notifications = listOf(
            MentionNotificationInfo(UserMentioned::class) { event ->
                event.run {
                    val team = if (hasViaTeam()) viaTeam else null
                    MentionDetails(
                        title, whenMentioned, whoMentioned.username, whereMentioned, team
                    )
                }
            },
            MentionNotificationInfo(MentionUnsnoozed::class) { event ->
                event.run {
                    val team = if (hasViaTeam()) viaTeam else null
                    MentionDetails(
                        title, whenMentioned, whoMentioned.username, whereMentioned, team
                    )
                }
            }
        )
    }

    /**
     * Enables notifications for new and unsnoozed mentions
     * for the provided client with the given username.
     *
     * @param client Enables subscription to events emitted by the Pingh server.
     * @param username The username for which mentions will be sent.
     */
    internal fun enableMentionNotifications(client: DesktopClient, username: Username) {
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
        notification: MentionNotificationInfo<E>
    ) {
        client.observeEvent(
            notification.onEvent,
            eq(usernameField(), username)
        ) { event ->
            val details = notification.extractDetailsFrom(event)
            if (settings.current.run { !dndEnabled && !isIgnored(details.whereMentioned) }) {
                alert.notifyMention(details)
            }
        }
    }

    /**
     * Returns the subscribable field of the username extracted from the mention ID.
     */
    private fun usernameField(): EventMessageField =
        EventMessageField(Field.named("id").nested("user"))

    /**
     * Creates and sends a session expiration notification
     * if 'Do Not Disturb' mode is disabled.
     */
    internal fun notifySessionExpired() {
        if (!settings.current.dndEnabled) {
            alert.notifySessionExpired()
        }
    }

    /**
     * Information about available notification.
     *
     * @param E The type of the event that triggers the sending of a notification.
     *
     * @property onEvent The event that triggers the sending of a notification.
     * @property extractDetailsFrom Retrieves mention details from the mention event.
     */
    private data class MentionNotificationInfo<E : EventMessage>(
        val onEvent: KClass<E>,
        val extractDetailsFrom: (E) -> MentionDetails
    )
}
