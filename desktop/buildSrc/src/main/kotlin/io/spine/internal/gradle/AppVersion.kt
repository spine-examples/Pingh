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

package io.spine.internal.gradle

/**
 * The version of the application.
 *
 * @property value The string value of the version.
 */
public data class AppVersion(
    public val value: String
)

/**
 * Extracts the semantic version from the string, including the major version,
 * and optionally the minor and patch versions. Numbers must be separated by dots.
 *
 * @receiver The version of the application that contains the semantic version.
 * @throws IllegalArgumentException if this version does not contain a semantic version.
 * @see <a href="https://semver.org/">Semantic Versioning</a>
 */
public fun AppVersion.extractSemanticVersion(): AppVersion {
    val pattern = """\d+(.\d+){0,2}""".toRegex()
    val matchResult = pattern.find(value)
        ?: throw IllegalArgumentException(
            "$value does not contain semantic version: ${pattern.pattern}."
        )
    return AppVersion(matchResult.value)
}
