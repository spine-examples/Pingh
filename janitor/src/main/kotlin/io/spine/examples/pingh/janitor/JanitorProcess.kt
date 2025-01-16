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

import io.spine.base.EntityState
import io.spine.logging.Logging
import io.spine.protobuf.ValidatingBuilder
import io.spine.server.procman.ProcessManager

/**
 * Abstract base for process managers responsible
 * for deleting entity records marked as archived or deleted from storage.
 *
 * Only one janitor is needed the bounded context.
 * The identifier should be the name of the context,
 * and the repositories containing the entities to be deleted must be provided.
 *
 * @param S The type of the process manager state.
 * @param B The type of the builder of the process manager state.
 */
public abstract class JanitorProcess<S : EntityState, B : ValidatingBuilder<S>> :
    ProcessManager<JanitorId, S, B>(), Logging {

    /**
     * List of repositories that need to be cleared of archived and deleted entity records.
     *
     * It is expected this field is set by calling [inject()][inject]
     * right after the instance creation.
     */
    private lateinit var purgeableRepos: List<Purgeable>

    /**
     * Deletes all entity records marked as archived or deleted
     * from the [repositories][purgeableRepos] that this Janitor is responsible for.
     */
    protected fun purge() {
        _debug().log("${id().forLog()}: Deleting obsolete records...")
        purgeableRepos.forEach { it.purge() }
        _debug().log("${id().forLog()}: Obsolete records were successfully deleted..")
    }

    /**
     * Supplies this instance with list of repositories that need
     * to be cleared of archived and deleted entity records.
     *
     * It is expected this method is called right after the creation of the process instance.
     * Otherwise, the process will not be able to function properly.
     */
    public fun inject(repos: List<Purgeable>) {
        purgeableRepos = repos
    }
}
