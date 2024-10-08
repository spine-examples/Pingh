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

import io.spine.internal.BuildSettings
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.DokkaBaseConfiguration
import org.jetbrains.dokka.gradle.DokkaTask
import java.time.LocalDate

plugins {
    id("org.jetbrains.dokka")
}

/**
 * Configures [DokkaTask]s to accept Kotlin files and customizes output files.
 */
tasks.withType<DokkaTask>().configureEach {
    dokkaSourceSets.configureEach {
        // Configures links to the external Java documentation.
        jdkVersion.set(BuildSettings.javaVersion.asInt())

        skipEmptyPackages.set(true)

        documentedVisibilities.set(
            setOf(
                DokkaConfiguration.Visibility.PUBLIC,
                DokkaConfiguration.Visibility.PROTECTED
            )
        )

        // Dokka Base plugin allows to set a few properties to customize the output:
        //
        // - `separateInheritedMembers` when set to `true`, creates a separate tab in
        //  type-documentation for inherited members;
        // - `footerMessage` property to specify footer text in page generated by Dokka.
        //
        // See Dokka modifying frontend assets:
        // https://kotlinlang.org/docs/dokka-html.html#customize-assets.
        //
        pluginConfiguration<DokkaBase, DokkaBaseConfiguration> {
            separateInheritedMembers = true
            footerMessage = "Copyright ${LocalDate.now().year}, TeamDev"
        }
    }

    // Specifies output directory for documents generated by the Dokka.
    val buildDirectory = layout.buildDirectory.asFile.get().path
    outputDirectory.set(file("$buildDirectory/docs/dokkaKotlin"))
}
