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

import io.spine.examples.pingh.github.Organization
import io.spine.examples.pingh.github.loggedAs
import java.util.Properties

/**
 * Organizations whose members are allowed to authorize with the Pingh app.
 *
 * For this to work correctly, the organization must have
 * the [Pingh application](https://github.com/apps/pingh-tracker-of-github-mentions)
 * installed on GitHub.
 */
internal object PermittedOrganizations {
    /**
     * List of names of permitted organizations.
     */
    private val orgs: Set<Organization> = loadFromProperties()

    /**
     * Loads the list of permitted organizations from resource folder.
     */
    private fun loadFromProperties(): Set<Organization> {
        val path = "/config/auth.properties"
        val properties = Properties()
        PermittedOrganizations::class.java.getResourceAsStream(path).use {
            properties.load(it)
        }
        val orgStr = properties.getProperty("permitted-organizations")
            ?: throw IllegalStateException(
                "List of names of permitted organizations must be provided " +
                        "in the configuration file located at \"resource$path\"."
            )
        return orgStr.split(""",\s*""".toRegex())
            .map { Organization::class.loggedAs(it.trim()) }
            .toSet()
    }

    /**
     * Returns `true` if a member of this organization is permitted to log in to the application.
     */
    internal fun contains(org: Organization): Boolean = orgs.contains(org)
}
