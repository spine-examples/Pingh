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

import com.google.protobuf.Timestamp
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * A time format, where the day of the month and the name of the month itself come first,
 * followed by the hours and minutes.
 */
private val datetimeFormat = DateTimeFormatter.ofPattern("dd MMM HH:mm")

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
 * Converts `Timestamp` to the `dd MMM HH:mm` string.
 *
 * Examples:
 * - `10 May 14:37`
 * - `05 Sep 04:00`
 */
internal fun Timestamp.toDatetime(): String =
    datetimeFormat.format(toLocalDateTime())

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
 * @throws IllegalArgumentException if the `Timestamp` is not from the past.
 */
internal fun Timestamp.howMuchTimeHasPassed(): String {
    val thisDatetime = this.toLocalDateTime()
    val difference = Duration.between(
        thisDatetime,
        LocalDateTime.now(ZoneOffset.UTC)
    )
    require(difference > Duration.ZERO) { "" }
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
 * Converts this `Timestamp` to `LocalDateTime`.
 */
private fun Timestamp.toLocalDateTime(): LocalDateTime =
    LocalDateTime.ofEpochSecond(this.seconds, this.nanos, ZoneOffset.UTC)
