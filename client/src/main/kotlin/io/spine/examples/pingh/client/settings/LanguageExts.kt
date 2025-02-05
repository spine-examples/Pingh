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

package io.spine.examples.pingh.client.settings

import java.util.Locale
import kotlin.reflect.KClass

/**
 * A language name in its native form.
 */
public val Language.nativeName: String
    get() = options[this]?.nativeName ?: "English"

/**
 * The list of languages currently supported by the app.
 */
@Suppress("UnusedReceiverParameter" /* Associated with the class but doesn't use its data. */)
public val KClass<Language>.supported: List<Language>
    get() = options.keys.toList().sortedBy { it.nativeName }

/**
 * An ISO-639 alpha-2 or alpha-3 language code.
 */
internal val Language.code: String
    get() = options[this]?.code ?: "en"

/**
 * Returns a `Locale` representing a specific `Language`.
 */
internal fun Language.toLocale(): Locale = Locale(code)

/**
 * Creates a new `Language` by the passed `Locale`.
 *
 * If the `Locale` specifies an unsupported language, `null` is returned.
 */
internal fun KClass<Language>.by(locale: Locale): Language? {
    supported.forEach { language ->
        if (locale.language.equals(Locale(language.code).language)) {
            return language
        }
    }
    return null
}

private val options = mapOf(
    Language.ENGLISH to LanguageOption("English", "en"),
    Language.GERMAN to LanguageOption("Deutsch", "de"),
    Language.SPANISH to LanguageOption("Español", "es")
)

/**
 * Additional information related to [Language].
 *
 * @property nativeName The language name in its native form.
 * @property code An ISO-639 alpha-2 or alpha-3 language code.
 */
private class LanguageOption(
    val nativeName: String,
    val code: String
)
