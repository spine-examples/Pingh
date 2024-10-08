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

package io.spine.examples.pingh.client

import com.google.protobuf.Timestamp
import com.google.protobuf.util.Timestamps
import io.spine.time.InstantConverter
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * A time format consisting of the day of the month and the name of the month.
 */
private val dateFormat = DateTimeFormatter.ofPattern("dd MMM")

/**
 * Number of minutes in one hour.
 */
private const val minutesPerHour = 60L

/**
 * Number of hours in one day.
 */
private const val hoursPerDay = 24L

/**
 * Adds a duration to this timestamp.
 */
internal fun Timestamp.add(duration: com.google.protobuf.Duration): Timestamp =
    Timestamps.add(this, duration)

/**
 * Converts a `Timestamp` to a string based on the time difference between
 * the given past timestamp and the current moment.
 *
 * - If the difference is less than a minute, returns the string `"just now"`;
 * - If the difference is between one minute and one hour, returns the difference in minutes;
 * - If the difference is between one hour and one day, returns the difference in hours;
 * - If the difference is exactly one day, returns the string `"yesterday"`;
 * - In all other cases, returns the day and month.
 *
 * @receiver The timestamp represents a time in the past.
 * @throws IllegalArgumentException if the `Timestamp` is not from the past.
 */
public fun Timestamp.howMuchTimeHasPassed(): String {
    val thisDatetime = this.asLocalDateTime()
    val now = LocalDateTime.now()
    val difference = Duration.between(thisDatetime, now)
    require(difference > Duration.ZERO) {
        "The provided `Timestamp`, $thisDatetime, must represent a time in the past, " +
                "but it does not. The current time is $now."
    }
    return when {
        difference.toMinutes() < 1L -> "just now"
        difference.toMinutes() == 1L -> "a minute ago"
        difference.toMinutes() < minutesPerHour -> "${difference.toMinutes()} minutes ago"
        difference.toHours() == 1L -> "an hour ago"
        difference.toHours() < hoursPerDay -> "${difference.toHours()} hours ago"
        difference.toDays() == 1L -> "yesterday"
        else -> dateFormat.format(thisDatetime)
    }
}

/**
 * Converts the current UTC time in this `Timestamp` to local time,
 * based on the system's time zone.
 *
 * The default time zone is set to the [ZoneId.systemDefault()][ZoneId.systemDefault] zone.
 */
public fun Timestamp.asLocalDateTime(timeZone: ZoneId = ZoneId.systemDefault()): LocalDateTime {
    val instant = InstantConverter.reversed()
        .convert(this)
    return LocalDateTime.ofInstant(instant, timeZone)
}
