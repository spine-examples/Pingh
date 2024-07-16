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

package io.spine.examples.pingh.desktop.home

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import io.spine.examples.pingh.client.DesktopClient
import io.spine.examples.pingh.mentions.MentionId
import io.spine.examples.pingh.mentions.MentionStatus
import java.lang.Thread.sleep

/**
 * UI Model for the [HomePage].
 *
 * UI Model is a layer between `@Composable` functions and client.
 */
public class HomePageModel(private val client: DesktopClient) {

    /**
     * User mentions.
     */
    private var mentions: MutableState<MentionsList> = mutableStateOf(client.findUserMentions())

    /**
     * Returns all user mentions.
     */
    public fun mentions(): MentionsList = mentions.value

    /**
     * Updates the user mentions.
     */
    public fun updateMentions() {
        client.updateMentions(
            onSuccess = {
                sleep(100) // Makes a small delay before reading mentions
                // so that the read-side on the server can be updated.
                mentions.value = client.findUserMentions()
            }
        )
    }

    /**
     * Marks the mention as snoozed.
     */
    public fun markMentionAsSnoozed(id: MentionId) {
        client.markMentionAsSnoozed(id) {
            mentions.value = mentions.value.setMentionStatus(id, MentionStatus.SNOOZED)
        }
    }

    /**
     * Marks that the mention is read.
     */
    public fun markMentionAsRead(id: MentionId) {
        client.markMentionAsRead(id) {
            mentions.value = mentions.value.setMentionStatus(id, MentionStatus.READ)
        }
    }
}
