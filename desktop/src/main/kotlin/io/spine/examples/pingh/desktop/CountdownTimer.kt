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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay

/**
 * Displays a countdown timer that updates every second, starting from "`minutes`:`seconds`"
 * and ending at "00:00".
 *
 * The timer includes two components:
 *
 * 1. A circular progress indicator that visually represents the time left.
 * 2. A text displaying the exact time remaining in `mm:ss` format.
 *
 * The timer starts automatically when the component enters the composition
 * and stops when it exits.
 *
 * @param minutes The number of minutes to count down.
 * @param seconds The number of seconds to count down.
 * @param size The timer size.
 * @param indicatorColor The color of this progress indicator.
 * @param trackColor The color of the track behind the indicator.
 */
@Composable
internal fun CountdownTimer(
    minutes: Int,
    seconds: Int,
    size: Dp,
    indicatorColor: Color,
    trackColor: Color
) {
    require(minutes >= 0) { "The number of minutes must be non-negative." }
    require(seconds >= 0) { "The number of seconds must be non-negative." }
    val fullTime = TimeUnit(minutes, seconds)
    val time by produceState(initialValue = fullTime) {
        while (value.inWholeSeconds > 0) {
            delay(1.seconds)
            value = value.minusSecond()
        }
    }
    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { time.inWholeSeconds.toFloat() / fullTime.inWholeSeconds },
            modifier = Modifier.fillMaxSize(),
            color = indicatorColor,
            trackColor = trackColor,
            strokeCap = StrokeCap.Round
        )
        Text(
            text = "$time",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}



/**
 * A time representation in minutes and seconds for use in countdown timers.
 */
private data class TimeUnit(val min: Int, val sec: Int) {

    val inWholeSeconds: Int
        get() = min * secPerMin + sec

    @Suppress("MagicNumber" /* Uses for ensure "mm:ss" time format. */)
    override fun toString(): String {
        val mins = if (min < 10) "0$min" else "$min"
        val secs = if (sec < 10) "0$sec" else "$sec"
        return "$mins:$secs"
    }

    fun minusSecond(): TimeUnit {
        check(inWholeSeconds > 0) { "Time has already elapsed." }
        val nextSec = sec - 1
        return if (nextSec < 0) {
             TimeUnit(min - 1, secPerMin - 1)
        } else {
            TimeUnit(min, nextSec)
        }
    }

    private companion object {
        private const val secPerMin = 60
    }
}
