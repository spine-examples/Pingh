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

package io.spine.examples.pingh.mentions

import com.google.protobuf.Duration
import com.google.protobuf.Timestamp
import com.google.protobuf.util.Timestamps
import com.google.protobuf.util.Timestamps.between
import io.spine.base.EventMessage
import io.spine.base.Time.currentTime
import io.spine.core.External
import io.spine.examples.pingh.clock.event.TimePassed
import io.spine.examples.pingh.github.Mention
import io.spine.examples.pingh.github.PersonalAccessToken
import io.spine.examples.pingh.github.Username
import io.spine.examples.pingh.mentions.command.UpdateMentionsFromGitHub
import io.spine.examples.pingh.mentions.event.GitHubTokenUpdated
import io.spine.examples.pingh.mentions.event.MentionsUpdateFromGitHubCompleted
import io.spine.examples.pingh.mentions.event.MentionsUpdateFromGitHubRequested
import io.spine.examples.pingh.mentions.event.RequestMentionsFromGitHubFailed
import io.spine.examples.pingh.mentions.event.UserMentioned
import io.spine.examples.pingh.mentions.rejection.MentionsUpdateIsAlreadyInProgress
import io.spine.examples.pingh.sessions.event.TokenUpdated
import io.spine.examples.pingh.sessions.event.UserLoggedIn
import io.spine.protobuf.Durations2.minutes
import io.spine.protobuf.Durations2.toNanos
import io.spine.server.command.Assign
import io.spine.server.command.Command
import io.spine.server.event.React
import io.spine.server.procman.ProcessManager
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.Optional
import kotlin.jvm.Throws
import kotlin.reflect.KClass

/**
 * The time interval between automatic requests to update mentions.
 */
internal val mentionsUpdateInterval: Duration = minutes(1)

/**
 * The limit the number of mentions loaded on the first launch.
 */
private const val limitOnFirstLaunch: Int = 20

/**
 * A process of reading user's mentions from GitHub.
 */
internal class GitHubClientProcess :
    ProcessManager<GitHubClientId, GitHubClient, GitHubClient.Builder>() {

    /**
     * Service that fetches mentions from GitHub.
     *
     * It is expected this field is set by calling [inject]
     * right after the instance creation.
     */
    private lateinit var search: GitHubSearch

    /**
     * Updates the user's [PersonalAccessToken] each time the user logs in.
     */
    @React
    internal fun on(@External event: UserLoggedIn): GitHubTokenUpdated {
        builder().setToken(event.token)
        return GitHubTokenUpdated::class.buildBy(
            GitHubClientId::class.of(event.id.username),
            event.token
        )
    }

    /**
     * Updates the user's [PersonalAccessToken] each time it is refreshed.
     */
    @React
    internal fun on(@External event: TokenUpdated): GitHubTokenUpdated {
        builder().setToken(event.token)
        return GitHubTokenUpdated::class.buildBy(
            GitHubClientId::class.of(event.id.username),
            event.token
        )
    }

    /**
     * Starts the process of fetching mentions from GitHub upon the first login.
     *
     * If no updates have been completed and none are in progress,
     * indicating that mentions have not yet been fetched,
     * an `UpdateMentionsFromGitHub` command will be sent.
     * Otherwise, an empty `Optional` will be returned.
     */
    @Command
    internal fun on(event: GitHubTokenUpdated): Optional<UpdateMentionsFromGitHub> {
        if (!state().hasWhenLastSuccessfullyUpdated() && !state().hasWhenStarted()) {
            return Optional.of(UpdateMentionsFromGitHub::class.buildBy(event.id, currentTime()))
        }
        return Optional.empty()
    }

    /**
     * Sends a `UpdateMentionsFromGitHub` command if the time elapsed since
     * the last successful update exceeds the required [interval][mentionsUpdateInterval].
     *
     * If no updates have been made yet, the command will be sent regardless.
     */
    @Command
    internal fun on(@External event: TimePassed): Optional<UpdateMentionsFromGitHub> {
        val currentTime = event.time
        val difference = between(state().whenLastSuccessfullyUpdated, currentTime)
        if (!state().hasWhenLastSuccessfullyUpdated() || difference >= mentionsUpdateInterval) {
            return Optional.of(UpdateMentionsFromGitHub::class.buildBy(state().id, currentTime))
        }
        return Optional.empty()
    }

    /**
     * Starts the process of updating mentions for the user.
     *
     * When a mention update is requested for a user, checks whether the previous update
     * has ended. If this condition is met, the process of updating the mentions from GitHub
     * is started.
     */
    @Assign
    @Throws(MentionsUpdateIsAlreadyInProgress::class)
    internal fun handle(command: UpdateMentionsFromGitHub): MentionsUpdateFromGitHubRequested {
        if (state().hasWhenStarted()) {
            throw MentionsUpdateIsAlreadyInProgress::class.buildBy(command.id)
        }
        builder().setWhenStarted(command.whenRequested)
        return MentionsUpdateFromGitHubRequested::class.buildBy(state().id)
    }

    /**
     * Fetches user's mentions from GitHub and terminates the mention update process.
     *
     * If mentions have never been loaded before, it retrieves no more 20 mentions made
     * since the start of the previous workday. Otherwise, it fetches all mentions
     * since the last update.
     *
     * @return If the mentions fetching from GitHub is successful, list of events,
     * where the [UserMentioned] event for each mention comes first,
     * followed by a single [MentionsUpdateFromGitHubCompleted] event.
     * Otherwise, the list is one [RequestMentionsFromGitHubFailed] event.
     */
    @React
    internal fun on(event: MentionsUpdateFromGitHubRequested): List<EventMessage> {
        val username = state().id.username
        val token = state().token
        val updatedAfter = state().whenLastSuccessfullyUpdated.thisOrLastWorkday()
        val mentions = try {
            if (state().whenLastSuccessfullyUpdated.isDefault()) {
                search.searchMentions(username, token, updatedAfter, limitOnFirstLaunch)
            } else {
                search.searchMentions(username, token, updatedAfter)
            }
        } catch (exception: CannotObtainMentionsException) {
            builder().clearWhenStarted()
            return listOf(
                RequestMentionsFromGitHubFailed::class.buildBy(event.id, exception.statusCode())
            )
        }
        val userMentionedEvents = toEvents(mentions, state().id.username)
        val mentionsUpdateFromGitHubCompleted =
            MentionsUpdateFromGitHubCompleted::class.buildBy(event.id)
        builder()
            .setWhenLastSuccessfullyUpdated(state().whenStarted)
            .clearWhenStarted()
        return userMentionedEvents
            .toList<EventMessage>()
            .plus(mentionsUpdateFromGitHubCompleted)
    }

    /**
     * Supplies this instance of the process with a service allowing to access GitHub.
     *
     * It is expected this method is called right after the creation of the process instance.
     * Otherwise, the process will not be able to function properly.
     */
    internal fun inject(search: GitHubSearch) {
        this.search = search
    }

    private companion object {
        /**
         * Converts the set of `Mention`s to the set of `UserMentioned` events
         * with the specified name of the mentioned user.
         */
        private fun toEvents(gitHubMentions: Set<Mention>, whoWasMentioned: Username):
                Set<UserMentioned> =
            gitHubMentions
                .map { mention -> UserMentioned::class.buildBy(mention, whoWasMentioned) }
                .toSet()
    }
}

/**
 * Returns this `Timestamp` if its value is not default,
 * otherwise returns the midnight of the last workday.
 *
 * @see [identifyLastWorkday]
 */
private fun Timestamp.thisOrLastWorkday(): Timestamp =
    if (isDefault()) {
        Timestamp::class.identifyLastWorkday()
    } else {
        this
    }

/**
 * Returns the midnight of the last working day, if counted from the current point in time.
 *
 * Working days are days from Monday to Friday, inclusive.
 *
 * If today is a workday, returns midnight today.
 */
@Suppress("UnusedReceiverParameter" /* Class extension doesn't use class as a parameter. */)
public fun KClass<Timestamp>.identifyLastWorkday(): Timestamp {
    var localDateTime = LocalDateTime.of(LocalDate.now(), LocalTime.MIN)
    while (localDateTime.dayOfWeek in listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)) {
        localDateTime = localDateTime.minusDays(1)
    }
    return Timestamps.fromSeconds(localDateTime.toEpochSecond(ZoneOffset.UTC))
}

/**
 * Compares this `Duration` with the passed one.
 */
private operator fun Duration.compareTo(other: Duration): Int =
    toNanos(this).compareTo(toNanos(other))
