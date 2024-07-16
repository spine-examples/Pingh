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

import com.google.protobuf.util.Timestamps
import io.spine.examples.pingh.mentions.MentionId
import io.spine.examples.pingh.mentions.MentionStatus
import io.spine.examples.pingh.mentions.MentionView

/**
 * Order of statuses for sorting mentions in the [sortByStatusAndWhenMentioned] method.
 */
private val statusOrder = mapOf(
    MentionStatus.UNREAD to 0,
    MentionStatus.SNOOZED to 1,
    MentionStatus.READ to 2
)

/**
 * List of `MentionsView`
 */
internal typealias MentionsList = List<MentionView>

/**
 * Creates a new list by replacing the status of one mention with another.
 */
internal fun MentionsList.setMentionStatus(id: MentionId, status: MentionStatus): MentionsList {
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
 * followed by snoozed mentions, and read mentions at the end.
 *
 * Within each group, mentions are sorted by the time they were made.
 *
 * @see [statusOrder]
 */
internal fun MentionsList.sortByStatusAndWhenMentioned(): MentionsList =
    this.sortedWith { firstMentions, secondMentions ->
        val statusComparison = statusOrder[firstMentions.status]!!
            .compareTo(statusOrder[secondMentions.status]!!)
        if (statusComparison != 0) {
            statusComparison
        } else {
            Timestamps.compare(firstMentions.whenMentioned, secondMentions.whenMentioned)
        }
    }
