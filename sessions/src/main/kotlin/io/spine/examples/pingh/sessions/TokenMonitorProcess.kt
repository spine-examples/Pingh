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

package io.spine.examples.pingh.sessions

import io.spine.core.External
import io.spine.examples.pingh.clock.event.TimePassed
import io.spine.examples.pingh.sessions.command.UpdateToken
import io.spine.examples.pingh.sessions.event.TokenExpirationTimeUpdated
import io.spine.examples.pingh.sessions.event.TokenMonitoringFinished
import io.spine.examples.pingh.sessions.event.TokenMonitoringStarted
import io.spine.examples.pingh.sessions.event.TokenUpdated
import io.spine.examples.pingh.sessions.event.UserLoggedIn
import io.spine.examples.pingh.sessions.event.UserLoggedOut
import io.spine.protobuf.Durations2.minutes
import io.spine.server.command.Command
import io.spine.server.event.React
import io.spine.server.procman.ProcessManager
import java.util.Optional

/**
 * The interval for sending a follow-up command to update the token
 * if no update occurs after the previous command.
 */
private val updateRetryInterval = minutes(1)

/**
 * Monitors the GitHub access token and starts the token update process when it expires.
 *
 * @see [TokenMonitor]
 */
internal class TokenMonitorProcess :
    ProcessManager<TokenMonitorId, TokenMonitor, TokenMonitor.Builder>() {

    /**
     * Starts the process of monitoring token expiration.
     */
    @React
    internal fun on(event: UserLoggedIn): TokenMonitoringStarted {
        builder().setWhenExpires(event.whenTokenExpires)
        return TokenMonitoringStarted::class.with(
            TokenMonitorId::class.of(event.id)
        )
    }

    /**
     * Sends a `UpdateCommand` command if the token is expired.
     *
     * Once the update command is sent, the update is considered in progress
     * for the [duration][updateRetryInterval]. If no update occurs within this time,
     * the update command will be sent again.
     */
    @Command
    internal fun on(@External event: TimePassed): Optional<UpdateToken> {
        val time = event.time
        val inProcess = state().hasWhenUpdateRequested() &&
                state().whenUpdateRequested.add(updateRetryInterval) < time
        if (time < state().whenExpires || inProcess) {
            return Optional.empty()
        }
        builder().setWhenUpdateRequested(time)
        return Optional.of(UpdateToken::class.with(state().id.session, time))
    }

    /**
     * Sets the expiration time for the received token
     * and finishes the token update process.
     */
    @React
    internal fun on(event: TokenUpdated): TokenExpirationTimeUpdated {
        with(builder()) {
            whenExpires = event.whenTokenExpires
            clearWhenUpdateRequested()
        }
        return TokenExpirationTimeUpdated::class.with(state().id)
    }

    /**
     * Finishes the process of monitoring token expiration.
     */
    @React
    internal fun on(event: UserLoggedOut): TokenMonitoringFinished {
        deleted = true
        return TokenMonitoringFinished::class.with(
            TokenMonitorId::class.of(event.id)
        )
    }
}
