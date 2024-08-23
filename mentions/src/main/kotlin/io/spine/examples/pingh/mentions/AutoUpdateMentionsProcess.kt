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
import com.google.protobuf.util.Timestamps.between
import io.spine.core.External
import io.spine.examples.pingh.clock.event.TimePassed
import io.spine.examples.pingh.mentions.command.UpdateMentionsFromGitHub
import io.spine.examples.pingh.mentions.event.AutoUpdateMentionsStarted
import io.spine.examples.pingh.mentions.event.GitHubTokenUpdated
import io.spine.protobuf.Durations2.minutes
import io.spine.protobuf.Durations2.toNanos
import io.spine.server.command.Command
import io.spine.server.event.React
import io.spine.server.model.Nothing
import io.spine.server.procman.ProcessManager
import io.spine.server.tuple.EitherOf2
import java.util.Optional

/**
 * The time interval between automatic requests to update mentions.
 */
internal val mentionsUpdateInterval: Duration = minutes(1)

/**
 * Manages the automatic update of user mentions.
 */
internal class AutoUpdateMentionsProcess :
    ProcessManager<GitHubClientId, AutoUpdateMentions, AutoUpdateMentions.Builder>() {

    /**
     * Starts an automatic update of mentions as soon as the [GitHubClient] receives
     * an access token to make requests.
     *
     * @return `AutoUpdateMentionsStarted` event if no updates have been made;
     *   otherwise, `Nothing`.
     */
    @React
    internal fun on(event: GitHubTokenUpdated): EitherOf2<AutoUpdateMentionsStarted, Nothing> =
        if (state().hasWhenLastRequested()) {
            EitherOf2.withB(nothing())
        } else {
            EitherOf2.withA(AutoUpdateMentionsStarted::class.withId(event.id))
        }

    /**
     * Sends a `UpdateMentionsFromGitHub` command if the time elapsed since
     * the last request exceeds the required [interval][mentionsUpdateInterval].
     *
     * If no previous request has been made, the command will be sent regardless.
     */
    @Command
    internal fun on(@External event: TimePassed): Optional<UpdateMentionsFromGitHub> {
        val currentTime = event.time
        val difference = between(state().whenLastRequested, currentTime)
        if (!state().hasWhenLastRequested() || difference >= mentionsUpdateInterval) {
            builder().setWhenLastRequested(currentTime)
            return Optional.of(UpdateMentionsFromGitHub::class.buildBy(state().id, currentTime))
        }
        return Optional.empty()
    }
}

/**
 * Compares this `Duration` with the passed one.
 */
private operator fun Duration.compareTo(other: Duration): Int =
    toNanos(this).compareTo(toNanos(other))
