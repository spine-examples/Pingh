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

package io.spine.examples.pingh.janitor

import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper
import io.spine.base.EntityState
import io.spine.examples.pingh.clock.event.TimePassed
import io.spine.server.procman.ProcessManagerRepository
import io.spine.server.route.EventRouting

/**
 * Abstract base for repositories that manage instances of [JanitorProcess].
 *
 * The repository must be added to the bounded context of the repositories it will clean.
 *
 * Each bounded context should have a single janitor process,
 * and all `TimePassed` events are always [routed][setupEventRouting] to it.
 *
 * @param P The type of janitor process managers.
 * @param S The type of janitor process manager state messages.
 *
 * @property purgeableRepos Set of repositories that need to be cleared
 *   of archived and deleted entity records.
 */
public abstract class JanitorRepository<P : JanitorProcess<S, *>, S : EntityState>(
    private val purgeableRepos: Set<Purgeable>
) : ProcessManagerRepository<JanitorId, P, S>() {

    @OverridingMethodsMustInvokeSuper
    override fun setupEventRouting(routing: EventRouting<JanitorId>) {
        super.setupEventRouting(routing)
        routing.unicast(TimePassed::class.java) { _ -> id() }
    }

    @OverridingMethodsMustInvokeSuper
    override fun configure(processManager: P) {
        super.configure(processManager)
        processManager.inject(purgeableRepos)
    }

    /**
     * Returns the identifier of the unique janitor for this context.
     *
     * This method must be invoked only after the repository
     * has been registered with the bounded context.
     */
    private fun id(): JanitorId =
        JanitorId::class.forContext(context().name().value())
}
