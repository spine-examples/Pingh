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

package io.spine.internal.gradle.publishing

import java.net.URI
import org.gradle.api.artifacts.dsl.RepositoryHandler

/**
 * Adds maven repository of Pingh project hosted at GitHub Packages.
 *
 * Authentication is handled using the GitHub actor and GitHub token from
 * the environment variables.
 *
 * @see <a href="https://shorturl.at/AuxHg">Publishing packages to GitHub Packages</a>
 */
public fun RepositoryHandler.gitHubPackages() {
    maven {
        name = "GitHubPackages"
        url = URI("https://maven.pkg.github.com/spine-examples/Pingh")
        credentials {
            username = actor()
            password = token()
        }
    }
}

/**
 * Returns the GitHub actor from the environment variable if it exists;
 * otherwise, returns the default actor.
 */
private fun actor(): String = System.getenv("GITHUB_ACTOR") ?: "developers@spine.io"

/**
 * Returns the GitHub token from the environment variables if it exists;
 * otherwise, returns empty string.
 */
private fun token(): String = System.getenv("GITHUB_TOKEN") ?: ""
