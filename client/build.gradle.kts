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

import io.spine.internal.dependency.AppDirs
import io.spine.internal.dependency.Grpc
import io.spine.internal.dependency.Guava
import io.spine.internal.dependency.KotlinX
import io.spine.internal.dependency.Spine

plugins {
    // Add the Gradle plugin for bootstrapping projects built with Spine.
    // See: https://github.com/SpineEventEngine/bootstrap
    id("io.spine.tools.gradle.bootstrap").version("1.9.0")
}

spine {
    // Enable the code generation for the elements declared in Proto files.
    assembleModel()

    // Add and configure required dependencies for developing a Spine-based Java client.
    // See: https://github.com/SpineEventEngine/bootstrap#java-projects
    enableJava().client()
}

forceGrpcDependencies(configurations)

dependencies {
    // To work with `PinghApplication`, it is necessary to use value objects and IDs declared
    // in different bounded contexts. All necessary classes are collected in the `server` module.
    api(project(":server"))

    implementation(Guava.lib)
    implementation(Grpc.netty)
    implementation(Grpc.inprocess)
    implementation(KotlinX.Coroutines.core)
    implementation(AppDirs.lib)

    testImplementation(project(":testutil-client"))
    testImplementation(project(":testutil-mentions"))
    testImplementation(project(":testutil-sessions"))
    testImplementation(project(":clock"))
    testImplementation(Spine.server)
    testImplementation(Spine.GCloud.datastore)
    testImplementation(Spine.GCloud.testutil)
}
