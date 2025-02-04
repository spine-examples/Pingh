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

package io.spine.examples.pingh.client.settings

import com.google.protobuf.Duration
import com.google.protobuf.util.Durations
import io.spine.protobuf.Durations2.hours
import io.spine.protobuf.Durations2.minutes
import kotlin.reflect.KClass

/**
 * The duration corresponding to this interval.
 */
internal val SnoozeTime.value: Duration
    get() = durations[this] ?: Durations.ZERO

/**
 * The list of snooze intervals currently supported by the app.
 */
@Suppress("UnusedReceiverParameter" /* Associated with the class but doesn't use its data. */)
public val KClass<SnoozeTime>.supported: List<SnoozeTime>
    get() = durations.keys.toList()

@Suppress("MagicNumber" /* The durations are specified using numbers. */)
private val durations = mapOf(
    SnoozeTime.THIRTY_MINUTES to minutes(30),
    SnoozeTime.TWO_HOURS to hours(2),
    SnoozeTime.ONE_DAY to hours(24)
)
