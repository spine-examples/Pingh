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

import io.spine.examples.pingh.client.storage.FileLocation.Companion.inAppDir
import io.spine.examples.pingh.client.storage.FileStorage

/**
 * Manages application settings.
 *
 * All settings changes are saved to a file in the user's data directory,
 * ensuring persistence across application restarts.
 */
internal class SettingsManager {
    /**
     * A repository for storing current application settings.
     */
    private val storage = FileStorage<UserSettings>(inAppDir(".settings"))

    /**
     * The current application settings.
     */
    internal var current: UserSettings
        private set

    init {
        current = storage.loadOrDefault(UserSettings::parseFrom, default)
    }

    /**
     * Sets and saves updated application settings.
     */
    internal fun update(settings: UserSettings) {
        current = settings
        storage.save(settings)
    }

    private companion object {
        private val default = UserSettings.newBuilder()
            .setEnabledDndMode(false)
            .setSnoozeTime(SnoozeTime.TWO_HOURS)
            .vBuild()
    }
}
