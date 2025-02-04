/*
 * Copyright 2025, TeamDev. All rights reserved.
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

import androidx.compose.runtime.Composable
import io.spine.example.pingh.desktop.generated.resources.Res
import io.spine.example.pingh.desktop.generated.resources.day
import io.spine.example.pingh.desktop.generated.resources.hours
import io.spine.example.pingh.desktop.generated.resources.minutes
import io.spine.example.pingh.desktop.generated.resources.not_member_of_permitted_orgs
import io.spine.example.pingh.desktop.generated.resources.username_mismatch_format
import io.spine.examples.pingh.client.settings.SnoozeTime
import io.spine.examples.pingh.sessions.rejection.LoginFailureCause
import io.spine.examples.pingh.sessions.rejection.Rejections.NotMemberOfPermittedOrgs
import io.spine.examples.pingh.sessions.rejection.Rejections.UsernameMismatch
import org.jetbrains.compose.resources.stringResource

/**
 * Returns the text corresponding to the [snooze time][snoozeTime] interval.
 */
@Composable
internal fun stringResource(snoozeTime: SnoozeTime): String =
    when (snoozeTime) {
        SnoozeTime.THIRTY_MINUTES -> "30 ${stringResource(Res.string.minutes)}"
        SnoozeTime.TWO_HOURS -> "2 ${stringResource(Res.string.hours)}"
        SnoozeTime.ONE_DAY -> "1 ${stringResource(Res.string.day)}"
        else -> ""
    }

/**
 * Returns the text corresponding to the [cause][loginFailureCause] of the login failure.
 */
@Composable
internal fun stringResource(loginFailureCause: LoginFailureCause): String =
    when (loginFailureCause) {
        is UsernameMismatch -> stringResource(
            Res.string.username_mismatch_format,
            loginFailureCause.expectedUser.value,
            loginFailureCause.loggedInUser.value
        )

        is NotMemberOfPermittedOrgs -> stringResource(Res.string.not_member_of_permitted_orgs)

        else -> ""
    }
