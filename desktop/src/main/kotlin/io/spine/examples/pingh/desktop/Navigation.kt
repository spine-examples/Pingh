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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import io.spine.examples.pingh.client.PinghApplication
import io.spine.examples.pingh.desktop.Page.LOGIN
import io.spine.examples.pingh.desktop.Page.MENTIONS
import io.spine.examples.pingh.desktop.Page.SETTINGS

/**
 * Displays the current page of the application.
 *
 * @param application Manages the logic for the Pingh app.
 * @param exitApp Closes applications and aborts any tasks it is performing.
 */
@Composable
internal fun CurrentPage(application: PinghApplication, exitApp: () -> Unit) {
    val loggedIn by application.loggedIn.collectAsState()
    val currentPage = remember(loggedIn) {
        mutableStateOf(
            if (loggedIn) MENTIONS else LOGIN
        )
    }
    when (currentPage.value) {
        LOGIN -> LoginPage(
            flow = application.startLoginFlow(),
            toMentionsPage = {
                currentPage.value = MENTIONS
            }
        )

        MENTIONS -> MentionsPage(
            flow = application.startMentionsFlow(),
            toSettingsPage = {
                currentPage.value = SETTINGS
            },
            exitApp = exitApp
        )

        SETTINGS -> SettingsPage(
            flow = application.startSettingsFlow(),
            toMentionsPage = {
                currentPage.value = MENTIONS
            },
            toLoginPage = {
                currentPage.value = LOGIN
            }
        )
    }
}

/**
 * Pages in the application.
 */
private enum class Page {
    /**
     * GitHub account login page.
     */
    LOGIN,

    /**
     * Page displaying user mentions.
     */
    MENTIONS,

    /**
     * Page with account settings.
     */
    SETTINGS
}
