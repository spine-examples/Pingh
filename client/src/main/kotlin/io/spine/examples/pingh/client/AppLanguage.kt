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

package io.spine.examples.pingh.client

import io.spine.examples.pingh.client.settings.Language
import io.spine.examples.pingh.client.settings.Locale
import io.spine.examples.pingh.client.settings.by
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale as PlatformLocale

/**
 * Manages languages used by app.
 *
 * The application language is shared across all users,
 * meaning it remains unchanged when switching users.
 *
 * All modifications to the language are saved to a file in the user's data directory,
 * ensuring persistence across application restarts.
 */
internal class AppLanguage {
    /**
     * A repository for storing application language.
     */
    private val storage = FileStorage<Locale>(FileLocation.inAppDir(".language"))

    private val language: MutableStateFlow<Language>

    init {
        val message = storage.loadOrDefault(Locale::parseFrom) { default() }
        language = MutableStateFlow(message.language)
    }

    /**
     * Returns the locale based on the system's primary language if supported;
     * otherwise, defaults to English.
     */
    private fun default(): Locale =
        localeBy(Language::class.by(PlatformLocale.getDefault()) ?: Language.ENGLISH)

    /**
     * The current language state used by the app.
     */
    internal val state: StateFlow<Language> = language

    /**
     * Updates the current language to the specified [value].
     */
    internal fun update(value: Language) {
        language.value = value
        storage.save(localeBy(value))
    }

    /**
     * Creates a new `Locale` with the passed `Language`.
     */
    private fun localeBy(language: Language): Locale =
        Locale.newBuilder()
            .setLanguage(language)
            .vBuild()
}
