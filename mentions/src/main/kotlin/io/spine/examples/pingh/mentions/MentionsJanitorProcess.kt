/*
 * Copyright 2025, TeamDev. All rights reserved.
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

import com.google.protobuf.util.Durations.toNanos
import com.google.protobuf.util.Timestamps
import io.spine.core.External
import io.spine.examples.pingh.clock.event.TimePassed
import io.spine.examples.pingh.janitor.JanitorProcess
import io.spine.examples.pingh.mentions.event.StoragePurged
import io.spine.protobuf.Durations2.hours
import io.spine.server.event.React
import java.util.Optional

/**
 * Removes entity records marked as archived or deleted
 * within the Mentions bounded context from the storage.
 *
 * @see [JanitorProcess]
 */
internal class MentionsJanitorProcess
    : JanitorProcess<MentionsJanitor, MentionsJanitor.Builder>() {

    /**
     * Purges repositories if [sufficient][cleanupInterval] time has passed since the last cleanup.
     *
     * If no cleanup has occurred yet, it is performed immediately.
     */
    @React
    internal fun on(@External event: TimePassed): Optional<StoragePurged> {
        if (state().hasLastLaunchTime()) {
            val diff = Timestamps.between(state().lastLaunchTime, event.time)
            if (toNanos(diff) <= toNanos(cleanupInterval)) {
                return Optional.empty()
            }
        }
        purge()
        builder().lastLaunchTime = event.time
        val purged = StoragePurged.newBuilder()
            .setId(id())
            .vBuild()
        return Optional.of(purged)
    }

    internal companion object {
        /**
         * The interval between cleanups of the storage.
         */
        internal val cleanupInterval = hours(1)
    }
}
