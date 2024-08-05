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

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.google.protobuf.util.Timestamps
import io.spine.base.Time.currentTime
import io.spine.examples.pingh.mentions.GitHubClientId
import io.spine.examples.pingh.mentions.MentionId
import io.spine.examples.pingh.mentions.MentionStatus
import io.spine.examples.pingh.mentions.MentionView
import io.spine.examples.pingh.mentions.UserMentions
import io.spine.examples.pingh.mentions.UserMentionsId
import io.spine.examples.pingh.mentions.buildBy
import io.spine.examples.pingh.mentions.command.MarkMentionAsRead
import io.spine.examples.pingh.mentions.command.SnoozeMention
import io.spine.examples.pingh.mentions.command.UpdateMentionsFromGitHub
import io.spine.examples.pingh.mentions.event.MentionRead
import io.spine.examples.pingh.mentions.event.MentionSnoozed
import io.spine.examples.pingh.mentions.event.MentionsUpdateFromGitHubCompleted
import java.lang.Thread.sleep

/**
 * The flow for managing the lifecycle of mentions.
 *
 * Enables updating mentions from GitHub, as well as reading and snoozing them.
 *
 * @param client enables interaction with the Pingh server.
 * @param session the information about the current user session.
 * @param settings the state of application settings.
 */
public class MentionsFlow internal constructor(
    private val client: DesktopClient,
    private val session: MutableState<UserSession?>,
    private val settings: SettingsState
) {
    private companion object {
        /**
         * Delay before reading mentions so that the read-side on the server can be updated.
         *
         * Time is specified in milliseconds.
         */
        private const val delayBeforeReadingMentions = 100L
    }

    /**
     * User mentions.
     */
    public var mentions: MutableState<MentionsList> = mutableStateOf(findUserMentions())
        private set

    /**
     * Updates the user's mentions.
     */
    public fun updateMentions() {
        ensureLoggedIn()
        val command = UpdateMentionsFromGitHub::class.buildBy(
            GitHubClientId::class.buildBy(session.value!!.username)
        )
        client.observeEventOnce(
            command.id,
            MentionsUpdateFromGitHubCompleted::class,
        ) {
            sleep(delayBeforeReadingMentions)
            mentions.value = findUserMentions()
        }
        client.send(command)
    }

    /**
     * Finds mentions of the user by their ID.
     *
     * @return List of `MentionView`s sorted by descending time of creation.
     */
    private fun findUserMentions(): List<MentionView> {
        ensureLoggedIn()
        val userMentions = client.readById(
            UserMentionsId::class.buildBy(session.value!!.username),
            UserMentions::class
        )
        if (userMentions.isEmpty()) {
            return listOf()
        }
        return userMentions[0]
            .mentionList
            .sortedByDescending { mention -> mention.whenMentioned.seconds }
    }

    /**
     * Marks the mention as snoozed.
     *
     * @param id the identifier of the mention that is marked as snoozed.
     */
    public fun markMentionAsSnoozed(
        id: MentionId
    ) {
        ensureLoggedIn()
        val command = SnoozeMention::class.buildBy(
            id,
            currentTime().add(settings.snoozeTime.value.value)
        )
        client.observeEventOnce(command.id, MentionSnoozed::class) {
            mentions.value = mentions.value.setMentionStatus(id, MentionStatus.SNOOZED)
        }
        client.send(command)
    }

    /**
     * Marks that the mention is read.
     *
     * @param id the identifier of the mention that is marked as read.
     */
    public fun markMentionAsRead(
        id: MentionId
    ) {
        ensureLoggedIn()
        val command = MarkMentionAsRead::class.buildBy(id)
        client.observeEventOnce(command.id, MentionRead::class) {
            mentions.value = mentions.value.setMentionStatus(id, MentionStatus.READ)
        }
        client.send(command)
    }

    /**
     * Throws `IllegalStateException` exception if the user is not logged in.
     */
    private fun ensureLoggedIn() {
        check(session.value != null) { "The user is not logged in." }
    }
}

/**
 * List of `MentionsView`s.
 */
public typealias MentionsList = List<MentionView>

/**
 * Updates the status of the mention with the specified identifier to the new status.
 *
 * @param id the identifier of the mention which the status was changed.
 * @param status new status of the mention.
 */
public fun MentionsList.setMentionStatus(
    id: MentionId,
    status: MentionStatus
): MentionsList {
    val idInList = this.indexOfFirst { it.id == id }
    val updatedMention = this[idInList]
        .toBuilder()
        .setStatus(status)
        .vBuild()
    val newMentionsList = this.toMutableList()
    newMentionsList[idInList] = updatedMention
    return newMentionsList
}

/**
 * Returns a `MentionsList` sorted such that unread mentions come first,
 * followed by snoozed mentions, and read mentions last.
 *
 * Within each group, mentions are sorted by the time they were made.
 */
public fun MentionsList.sortByStatusAndWhenMentioned(): MentionsList =
    this.sortedWith { firstMentions, secondMentions ->
        val statusComparison = firstMentions.status.compareTo(secondMentions.status)
        if (statusComparison != 0) {
            statusComparison
        } else {
            Timestamps.compare(secondMentions.whenMentioned, firstMentions.whenMentioned)
        }
    }