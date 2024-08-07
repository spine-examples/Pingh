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

import io.gitlab.arturbosch.detekt.Detekt

/**
 * This script-plugin sets up Kotlin code analyzing with Detekt.
 */
plugins {
    id("io.gitlab.arturbosch.detekt")
}

/**
 * After applying, Detekt is configured to use prepared `detekt-config.yml` file.
 */
detekt {
    buildUponDefaultConfig = true
    // The path to the Pingh project where the configuration for Detekt is stored.
    val pinghRootDir = rootProject.rootDir.parent
    config.from(files("$pinghRootDir/config/quality/detekt-config.yml"))
}

/**
 * Specifies that Detekt generates only HTML reports.
 */
tasks.withType<Detekt>().configureEach {
    reports {
        html.required.set(true)
        xml.required.set(false)
        txt.required.set(false)
        sarif.required.set(false)
        md.required.set(false)
    }
}
