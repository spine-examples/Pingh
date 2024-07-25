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

package io.spine.examples.pingh.desktop

/**
 * Max length of a GitHub username.
 */
private const val maxLengthOfUsername = 39

/**
 * Returns `true` if this `String` is valid `Username` value according to GitHub criteria;
 * otherwise, returns `false`.
 *
 * A valid GitHub username must:
 *
 * - Consist of alphanumeric characters and dashes (`'-'`);
 * - Not have consecutive dashes or dashes at the beginning or end;
 * - Not exceed 39 characters.
 *
 * @see <a href="https://docs.github.com/en/enterprise-server@3.9/admin/managing-iam/iam-configuration-reference/username-considerations-for-external-authentication">
 *     Username considerations for external authentication</a>
 */
@Suppress("ReturnCount") // To preserve the integrity of the algorithm,
// the number of `return` is exceeded.
internal fun String.isValidUsernameValue(): Boolean {
    if (this.length !in 1..maxLengthOfUsername) {
        return false
    }
    var previous = '-'
    this.forEach { current ->
        if (current.validateGiven(previous)) {
            return false
        }
        previous = current
    }
    return previous != '-'
}

/**
 * Returns `true` if this character is alphanumeric or
 * if it is a dash and `previous` is not a dash. Otherwise, returns `false`.
 */
private fun Char.validateGiven(previous: Char): Boolean =
    previous == '-' && this == '-'
            || !this.isAlphanumeric() && this != '-'

/**
 * Returns `true` if the character is a digit or an English letter in any case;
 * otherwise, returns `false`.
 */
private fun Char.isAlphanumeric(): Boolean =
    this in 'A'..'Z' || this in 'a'..'z' || this.isDigit()
