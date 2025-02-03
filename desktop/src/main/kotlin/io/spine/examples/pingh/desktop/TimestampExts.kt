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

import androidx.compose.ui.text.intl.Locale
import com.google.protobuf.Timestamp
import io.spine.example.pingh.desktop.generated.resources.Res
import io.spine.example.pingh.desktop.generated.resources.hour_ago
import io.spine.example.pingh.desktop.generated.resources.hours_ago_format
import io.spine.example.pingh.desktop.generated.resources.just_now
import io.spine.example.pingh.desktop.generated.resources.minute_ago
import io.spine.example.pingh.desktop.generated.resources.minutes_ago_format
import io.spine.example.pingh.desktop.generated.resources.yesterday
import io.spine.time.InstantConverter
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString

/**
 * Number of minutes in one hour.
 */
private const val minutesPerHour = 60L

/**
 * Number of hours in one day.
 */
private const val hoursPerDay = 24L

/**
 * A time format consisting of the day of the month and the name of the month.
 */
private val dateFormat = DateTimeFormatter.ofPattern("dd MMM")

/**
 * A time format, where the day of the month and the name of the month itself come first,
 * followed by the hours and minutes.
 */
private val datetimeFormat = DateTimeFormatter.ofPattern("dd MMM HH:mm")

/**
 * Converts `Timestamp` to the `dd MMM HH:mm` string.
 *
 * The month name is displayed in the current locale.
 *
 * Examples:
 * - `10 May 14:37`
 * - `05 Sep 04:00`
 */
internal fun Timestamp.toDatetime(timeZone: ZoneId = ZoneId.systemDefault()): String =
    datetimeFormat
        .localizedBy(Locale.current.platformLocale)
        .format(this.asLocalDateTime(timeZone))

/**
 * Converts a `Timestamp` to a localized string based on the time difference between
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
internal fun Timestamp.howMuchTimeHasPassed(): String {
    val thisDatetime = this.asLocalDateTime()
    val now = LocalDateTime.now()
    val difference = Duration.between(thisDatetime, now)
    require(difference > Duration.ZERO) {
        "The provided `Timestamp`, $thisDatetime, must represent a time in the past, " +
                "but it does not. The current time is $now."
    }
    return runBlocking {
        when {
            difference.toMinutes() < 1L -> getString(Res.string.just_now)

            difference.toMinutes() == 1L -> getString(Res.string.minute_ago)

            difference.toMinutes() < minutesPerHour ->
                getString(Res.string.minutes_ago_format, difference.toMinutes())

            difference.toHours() == 1L -> getString(Res.string.hour_ago)

            difference.toHours() < hoursPerDay ->
                getString(Res.string.hours_ago_format, difference.toHours())

            difference.toDays() == 1L -> getString(Res.string.yesterday)

            else -> dateFormat
                .localizedBy(Locale.current.platformLocale)
                .format(thisDatetime)
        }
    }
}

/**
 * Converts the current UTC time in this `Timestamp` to local time,
 * based on the system's time zone.
 *
 * The default time zone is set to the [ZoneId.systemDefault()][ZoneId.systemDefault] zone.
 */
private fun Timestamp.asLocalDateTime(timeZone: ZoneId = ZoneId.systemDefault()): LocalDateTime {
    val instant = InstantConverter.reversed()
        .convert(this)
    return LocalDateTime.ofInstant(instant, timeZone)
}
