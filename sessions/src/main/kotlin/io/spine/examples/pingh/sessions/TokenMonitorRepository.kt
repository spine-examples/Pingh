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

import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper
import io.spine.examples.pingh.clock.event.TimePassed
import io.spine.examples.pingh.sessions.event.SessionExpired
import io.spine.examples.pingh.sessions.event.TokenUpdated
import io.spine.examples.pingh.sessions.event.UserLoggedIn
import io.spine.examples.pingh.sessions.event.UserLoggedOut
import io.spine.server.procman.ProcessManagerRepository
import io.spine.server.route.EventRouting

/**
 * Manages instances of [TokenMonitorProcess].
 */
internal class TokenMonitorRepository :
    ProcessManagerRepository<TokenMonitorId, TokenMonitorProcess, TokenMonitor>() {

    @OverridingMethodsMustInvokeSuper
    override fun setupEventRouting(routing: EventRouting<TokenMonitorId>) {
        super.setupEventRouting(routing)
        routing
            .route(UserLoggedIn::class.java) { event, _ -> toTokenMonitor(event.id) }
            .route(TokenUpdated::class.java) { event, _ -> toTokenMonitor(event.id) }
            .route(UserLoggedOut::class.java) { event, _ -> toTokenMonitor(event.id) }
            .route(SessionExpired::class.java) { event, _ -> toTokenMonitor(event.id) }
            .route(TimePassed::class.java) { _, _ -> toAll() }
    }

    /**
     * Returns a set with a single token monitor ID,
     * that corresponds to the passed user session.
     */
    private fun toTokenMonitor(session: SessionId) =
        setOf(TokenMonitorId::class.of(session))

    /**
     * Returns a set of identifiers of records in the process manager storage.
     */
    private fun toAll(): Set<TokenMonitorId> =
        storage().index()
            .asSequence()
            .toSet()
}
