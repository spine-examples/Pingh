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

import com.google.protobuf.Timestamp
import io.spine.base.Time.currentTime
import io.spine.core.UserId
import io.spine.examples.pingh.clock.buildBy
import io.spine.examples.pingh.clock.event.TimePassed
import io.spine.examples.pingh.mentions.MentionProcess.Companion.lifetimeOfUnreadMention
import io.spine.examples.pingh.mentions.event.UserMentioned
import io.spine.examples.pingh.mentions.given.buildBy
import io.spine.examples.pingh.mentions.given.generate
import io.spine.examples.pingh.testing.mentions.given.PredefinedGitHubSearchResponses
import io.spine.examples.pingh.testing.sessions.given.PredefinedGitHubUsersResponses
import io.spine.protobuf.Durations2.seconds
import io.spine.server.BoundedContextBuilder
import io.spine.server.integration.ThirdPartyContext
import io.spine.testing.server.blackbox.ContextAwareTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`MentionsJanitor` should")
internal class MentionsJanitorSpec : ContextAwareTest() {

    private lateinit var mentionId: MentionId

    override fun contextBuilder(): BoundedContextBuilder = newMentionsContext(
        PredefinedGitHubSearchResponses(),
        PredefinedGitHubUsersResponses()
    )

    @BeforeEach
    internal fun createMention() {
        mentionId = MentionId::class.generate()
        val event = UserMentioned::class.buildBy(mentionId)
        context().receivesEvent(event)
        context().assertEntity(mentionId, MentionProcess::class.java)
            .exists()
    }

    @Test
    internal fun `physically removes mention marked as deleted`() {
        var time = currentTime().add(lifetimeOfUnreadMention).add(seconds(1))
        emitTimePassedEvent(time) // Deletes obsolete mention.
        time = time.add(MentionsJanitorProcess.cleanupInterval).add(seconds(1))
        emitTimePassedEvent(time) // Starts janitor.
        context().assertEntity(mentionId, MentionProcess::class.java)
            .doesNotExist()
    }

    private fun emitTimePassedEvent(time: Timestamp) {
        val clockContext = ThirdPartyContext.singleTenant("Clock")
        val event = TimePassed::class.buildBy(time)
        val actor = UserId::class.generate()
        clockContext.emittedEvent(event, actor)
    }
}
