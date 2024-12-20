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

package io.spine.examples.pingh.desktop.given

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.performMouseInput
import com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

/**
 * Causes the currently executing thread to sleep for the specified duration.
 */
internal fun delay(duration: Duration) {
    sleepUninterruptibly(duration.inWholeMilliseconds, TimeUnit.MILLISECONDS)
}

/**
 * Returns test tag attached to this semantics node.
 */
internal val SemanticsNode.testTag: String
    get() = config.getOrElse(SemanticsProperties.TestTag) {
        throw IllegalStateException("This node does not have a `TestTag` specified.")
    }

/**
 * Hovers the mouse pointer over the center of the element to trigger a hover event.
 */
@OptIn(ExperimentalTestApi::class)
internal fun SemanticsNodeInteraction.performHover() {
    val size = this.fetchSemanticsNode().size
    val middle = Offset(size.width / 2f, size.height / 2f)
    this.performMouseInput { this.moveTo(middle) }
}
