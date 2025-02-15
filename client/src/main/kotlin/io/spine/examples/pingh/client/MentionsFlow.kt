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

import com.google.protobuf.Duration
import com.google.protobuf.util.Timestamps
import io.spine.base.Time.currentTime
import io.spine.examples.pingh.client.settings.isIgnored
import io.spine.examples.pingh.client.settings.value
import io.spine.examples.pingh.mentions.GitHubClientId
import io.spine.examples.pingh.mentions.MentionId
import io.spine.examples.pingh.mentions.MentionStatus
import io.spine.examples.pingh.mentions.MentionView
import io.spine.examples.pingh.mentions.UserMentions
import io.spine.examples.pingh.mentions.UserMentionsId
import io.spine.examples.pingh.mentions.buildBy
import io.spine.examples.pingh.mentions.of
import io.spine.examples.pingh.mentions.command.MarkMentionAsRead
import io.spine.examples.pingh.mentions.command.PinMention
import io.spine.examples.pingh.mentions.command.SnoozeMention
import io.spine.examples.pingh.mentions.command.UnpinMention
import io.spine.examples.pingh.mentions.command.UpdateMentionsFromGitHub
import io.spine.examples.pingh.mentions.with
import io.spine.logging.Logging
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Controls the lifecycle of mentions and handles the user's action in relation to them.
 *
 * In particular:
 *
 * - subscribes to the user mentions projection, ensuring that any changes on
 *  the server are automatically updated in the application;
 * - whenever the user acts in relation to some mention, propagates these actions
 *  as commands to the server-side.
 *
 * @property client Enables interaction with the Pingh server.
 * @property session Manages the session with Pingh server.
 * @property settings Manages the application settings configured by a user.
 */
@Suppress("TooManyFunctions" /* Managing mentions requires numerous functions. */)
public class MentionsFlow internal constructor(
    private val client: DesktopClient,
    private val session: Session,
    private val settings: Settings
) : Logging {
    /**
     * User mentions.
     */
    public val mentions: MutableStateFlow<MentionsList> = MutableStateFlow(allMentions())

    init {
        subscribeToMentionsUpdates()
    }

    /**
     * Subscribes to updates for user mentions to automatically refresh
     * current [mentions] when changes occur on the server.
     */
    private fun subscribeToMentionsUpdates() {
        ensureLoggedIn()
        val id = UserMentionsId::class.of(session.username)
        client.observeEntity(id, UserMentions::class) { entity ->
            mentions.value = entity.mentionList
                .notIgnored()
                .distinctBy { it.id }
            _debug().log("Update to mentions on the server was made. The changes were applied.")
        }
        _debug().log("Subscribed to mention updates.")
    }

    /**
     * Requests the server to update the user's mentions from GitHub.
     *
     * Once updated, changes will be automatically applied,
     * as the flow [subscribes][subscribeToMentionsUpdates] to the user's mentions projection.
     */
    @Suppress("MemberVisibilityCanBePrivate" /* Accessed from `desktop` module. */)
    public fun updateMentions() {
        ensureLoggedIn()
        val command = UpdateMentionsFromGitHub::class.buildBy(
            GitHubClientId::class.of(session.username)
        )
        client.send(command)
        _debug().log("Requested an update for the mentions.")
    }

    /**
     * Finds mentions of the user by their ID.
     *
     * @return List of `MentionView`s sorted by descending time of creation.
     */
    public fun allMentions(): List<MentionView> {
        ensureLoggedIn()
        val userMentions = client.readById(
            UserMentionsId::class.of(session.username),
            UserMentions::class
        )
        return userMentions
            ?.mentionList
            ?.notIgnored()
            ?.distinctBy { it.id }
            ?: emptyList()
    }

    /**
     * Returns a list of mentions from repositories that the user does not ignore.
     */
    private fun MentionsList.notIgnored(): MentionsList =
        filter { mention -> !settings.current.isIgnored(mention.whereMentioned) }

    /**
     * Snoozes the mention.
     *
     * The duration for which a mention is snoozed is determined
     * by the app's [settings][SettingsState.snoozeTime].
     *
     * @param id The identifier of the mention to be snoozed.
     */
    public fun snooze(id: MentionId) {
        snooze(id, settings.current.snoozeTime.value)
    }

    /**
     * Snoozes the mention.
     *
     * @param id The identifier of the mention to be snoozed.
     * @param snoozeTime The duration of mention snooze.
     */
    internal fun snooze(id: MentionId, snoozeTime: Duration) {
        ensureLoggedIn()
        val command = SnoozeMention::class.buildBy(id, currentTime().add(snoozeTime))
        client.send(command)
        _debug().log("Requested to snooze the mention.")
    }

    /**
     * Marks the mention as read.
     *
     * @param id The identifier of the mention that is marked as read.
     */
    public fun markAsRead(id: MentionId) {
        ensureLoggedIn()
        val command = MarkMentionAsRead::class.buildBy(id)
        client.send(command)
        _debug().log("Requested to mark the mention as read.")
    }

    /**
     * Marks all unread and snoozed mentions as read.
     */
    public fun markAllAsRead() {
        ensureLoggedIn()
        mentions.value
            .filter { it.status != MentionStatus.READ }
            .forEach { markAsRead(it.id) }
        _debug().log("Requested to mark all mention as read.")
    }

    /**
     * Marks the mention as pinned.
     *
     * @param id The identifier of the mention that is marked as pinned.
     */
    public fun pin(id: MentionId) {
        ensureLoggedIn()
        val command = PinMention::class.with(id)
        client.send(command)
        _debug().log("Requested to pin the mention.")
    }

    /**
     * Marks the mention as unpinned.
     *
     * @param id The identifier of the mention that is marked as unpinned.
     */
    public fun unpin(id: MentionId) {
        ensureLoggedIn()
        val command = UnpinMention::class.with(id)
        client.send(command)
        _debug().log("Requested to unpin the mention.")
    }

    /**
     * Updates the flow state by applying the current application settings.
     */
    internal fun applySettings() {
        mentions.value = allMentions()
    }

    /**
     * Throws an `IllegalStateException` exception if the user is not logged in.
     */
    private fun ensureLoggedIn() {
        check(session.isActive) { "The user is not logged in." }
    }
}

/**
 * List of `MentionsView`s.
 */
public typealias MentionsList = List<MentionView>

/**
 * Returns a `MentionsList` sorted in the following order:
 * unread mentions first, followed by snoozed mentions, and then read mentions.
 *
 * Pinned mentions are placed before unpinned ones.
 *
 * Within each category, mentions are arranged chronologically by their creation time.
 */
public fun MentionsList.sorted(): MentionsList =
    this.sortedWith { firstMentions, secondMentions ->
        if (firstMentions.pinned xor secondMentions.pinned) {
            return@sortedWith compareValues(secondMentions.pinned, firstMentions.pinned)
        }
        val statusComparison = firstMentions.status.compareTo(secondMentions.status)
        if (statusComparison != 0) {
            statusComparison
        } else {
            Timestamps.compare(secondMentions.whenMentioned, firstMentions.whenMentioned)
        }
    }
