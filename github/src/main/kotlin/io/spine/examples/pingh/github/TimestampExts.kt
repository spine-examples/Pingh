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

package io.spine.examples.pingh.github

import com.google.protobuf.Timestamp
import com.google.protobuf.util.Timestamps
import io.spine.time.InstantConverter
import java.time.Instant
import java.time.format.DateTimeParseException
import kotlin.reflect.KClass

/**
 * Parses the time from a string in [ISO 8601](https://en.wikipedia.org/wiki/ISO_8601) format.
 *
 * Implementation details: The GitHub API provides time data in `ISO 8601` format,
 * while [Timestamps.parse()][Timestamps.parse] expects time data in
 * [RFC 3339](https://www.ietf.org/rfc/rfc3339.txt) format. To resolve this mismatch,
 * the input string is first parsed into an [Instant], since it matches the `ISO 8601` format.
 * The resulting `Instant` is then converted into a `Timestamp`.
 *
 * @throws DateTimeParseException if the provided string is not in `ISO 8601` format.
 */
@Suppress("UnusedReceiverParameter" /* Class extensions don't use class as a parameter. */)
internal fun KClass<Timestamp>.parse(value: String): Timestamp {
    val instant = Instant.parse(value)
    return InstantConverter.instance()
        .convert(instant)!!
}
