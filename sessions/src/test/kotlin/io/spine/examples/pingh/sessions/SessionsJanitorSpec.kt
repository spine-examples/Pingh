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

package io.spine.examples.pingh.sessions

import com.google.protobuf.Timestamp
import io.spine.base.Time.currentTime
import io.spine.examples.pingh.clock.buildBy
import io.spine.examples.pingh.clock.event.TimePassed
import io.spine.examples.pingh.github.PersonalAccessToken
import io.spine.examples.pingh.sessions.SessionsJanitorProcess.Companion.cleanupInterval
import io.spine.examples.pingh.sessions.UserSessionProcess.Companion.maxLoginTime
import io.spine.examples.pingh.sessions.command.LogUserIn
import io.spine.examples.pingh.sessions.event.UserLoggedIn
import io.spine.examples.pingh.sessions.event.UserLoggedOut
import io.spine.examples.pingh.sessions.given.generate
import io.spine.examples.pingh.testing.sessions.given.PredefinedGitHubAuthenticationResponses
import io.spine.examples.pingh.testing.sessions.given.PredefinedGitHubUsersResponses
import io.spine.protobuf.Durations2.seconds
import io.spine.server.BoundedContextBuilder
import io.spine.server.integration.ThirdPartyContext
import io.spine.testing.core.given.GivenUserId
import io.spine.testing.server.blackbox.ContextAwareTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`SessionsJanitor` should")
internal class SessionsJanitorSpec : ContextAwareTest() {

    override fun contextBuilder(): BoundedContextBuilder =
        SessionsContext(
            PredefinedGitHubAuthenticationResponses(),
            PredefinedGitHubUsersResponses()
        ).run {
            janitorEnabled = true
            newBuilder()
        }

    @Test
    internal fun `delete session marked as deleted`() {
        val id = SessionId::class.generate()
        val command = LogUserIn::class.withSession(id)
        context().receivesCommand(command)
        context().assertEntity(id, UserSessionProcess::class.java)
            .exists()

        var time = currentTime().add(maxLoginTime).add(seconds(1))
        emitTimePassedEvent(time) // Closes session due to expiration of login deadline.
        time = time.add(cleanupInterval).add(seconds(1))
        emitTimePassedEvent(time) // Starts janitor.
        context().assertEntity(id, UserSessionProcess::class.java)
            .doesNotExist()
    }

    @Test
    internal fun `delete token monitor process marked as deleted`() {
        val id = TokenMonitorId::class.of(SessionId::class.generate())
        val loggedIn = UserLoggedIn::class.with(
            id.session, PersonalAccessToken::class.generate(), currentTime()
        )
        context().receivesEvent(loggedIn)
        context().assertEntity(id, TokenMonitorProcess::class.java)
            .exists()

        val loggedOut = UserLoggedOut::class.with(id.session)
        context().receivesEvent(loggedOut)
        emitTimePassedEvent(currentTime())
        context().assertEntity(id, TokenMonitorProcess::class.java)
            .doesNotExist()
    }

    private fun emitTimePassedEvent(time: Timestamp) {
        val clockContext = ThirdPartyContext.singleTenant("Clock")
        val event = TimePassed::class.buildBy(time)
        val actor = GivenUserId.generated()
        clockContext.emittedEvent(event, actor)
    }
}
