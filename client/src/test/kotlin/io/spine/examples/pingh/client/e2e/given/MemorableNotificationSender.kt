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

package io.spine.examples.pingh.client.e2e.given

import io.kotest.matchers.shouldBe
import io.spine.examples.pingh.client.NotificationSender

/**
 * Memorizes the number of notifications that should is sent.
 *
 * Does not send any notifications. Use only for tests.
 */
internal class MemorableNotificationSender : NotificationSender {

    /**
     * The number of notifications that should is sent.
     */
    private var notificationsNumber = 0

    /**
     * Adds a notification to [total number][notificationsNumber] that should is sent.
     */
    override fun send(title: String, content: String) {
        notificationsNumber++
    }

    /**
     * Obtains the subject to check saved notifications.
     */
    internal fun assertNotifications(): NotificationsSubject =
        NotificationsSubject(notificationsNumber)
}

/**
 * Checks for notifications sent by the [MemorableNotificationSender].
 *
 * @param notificationsNumber the number of sent notifications.
 */
internal class NotificationsSubject(
    private val notificationsNumber: Int
) {

    /**
     * Fails if the notifications number does not have the given size.
     *
     * @param size the expected number of notifications.
     */
    internal fun hasSize(size: Int) {
        notificationsNumber shouldBe size
    }
}
