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

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import io.spine.internal.dependency.GCloud
import io.spine.internal.dependency.Grpc
import io.spine.internal.dependency.Guava
import io.spine.internal.dependency.Ktor
import io.spine.internal.dependency.Protobuf
import io.spine.internal.dependency.Spine
import io.spine.internal.dependency.Testcontainers
import io.spine.internal.gradle.publishing.gitHubPackages

plugins {
    // Add the Gradle plugin for bootstrapping projects built with Spine.
    // See: https://github.com/SpineEventEngine/bootstrap.
    id("io.spine.tools.gradle.bootstrap").version("1.9.0")

    // See: `ShadowJarPlugin` in `buildSrc/build.gradle.kts` file.
    id("com.github.johnrengelman.shadow")

    application
    `maven-publish`
}

forceGrpcDependencies(configurations)

spine {
    // Add and configure required dependencies for developing a Spine-based Java server.
    // See: https://github.com/SpineEventEngine/bootstrap#java-projects
    enableJava().server()
    forceDependencies = true
}

dependencies {
    api(project(":github"))
    api(project(":sessions"))
    api(project(":mentions"))
    implementation(project(":clock"))

    implementation(Ktor.Client.cio)
    implementation(Ktor.Server.core)
    implementation(Ktor.Server.netty)
    implementation(Guava.lib)
    implementation(Grpc.netty)
    implementation(Grpc.inprocess)
    implementation(GCloud.SecretManager.lib)
    implementation(GCloud.Datastore.lib)
    implementation(Protobuf.java)
    implementation(Protobuf.javaUtil)
    implementation(Spine.GCloud.datastore)
    implementation(Spine.GCloud.testutil)
    implementation(Testcontainers.lib)
    implementation(Testcontainers.gcloud)
}

/**
 * For Google Cloud Datastore to function correctly, Protobuf version 3.18 or higher is required.
 * Since Protobuf is included as a transitive dependency in many Google and Spine libraries,
 * it is necessary to explicitly specify the required version for the project.
 */
configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "com.google.protobuf" && requested.name == "protobuf-java") {
            useVersion(Protobuf.version)
        }
    }
}

val appClassName = "io.spine.examples.pingh.server.ServerKt"
project.setProperty("mainClassName", appClassName)

tasks.withType<ShadowJar> {
    mergeServiceFiles()
    mergeServiceFiles("desc.ref")
    mergeServiceFiles("META-INF/services/io.spine.option.OptionsProvider")
    manifest {
        attributes["Main-Class"] = appClassName
    }
    exclude(
        // Protobuf files.
        "google/**",
        "spine/**",
        "spine_examples/**"
    )
}

application {
    mainClass.set(appClassName)
}

/**
 * The name of the fat server artifact to be published to GitHub Packages.
 */
val fatArtifact = "pingh-server"

publishing {
    repositories {
        gitHubPackages()
    }

    publications {
        create("fatJar", MavenPublication::class) {
            groupId = project.group.toString()
            artifactId = fatArtifact
            version = project.version.toString()
            description = "Pingh app server."

            artifact(tasks.shadowJar) {
                // Avoid `-all` suffix in the published artifact.
                classifier = ""
            }
        }
    }
}

/**
 * Configures the publishing, so that:
 *
 * 1. The fat JAR artifact appears only in the remote Maven repository,
 * which is GitHub Packages in this case.
 *
 * 2. The `server` module is only published locally,
 * since it is needed for the project assembly.
 *
 * We use `afterEvaluate` because
 * the publishing tasks to modify are only available at this phase.
 */
afterEvaluate {
    tasks.withType<PublishToMavenRepository> {
        if (publication.artifactId != fatArtifact) {
            enabled = false
        }
    }
    tasks.withType<PublishToMavenLocal> {
        if (publication.artifactId == fatArtifact) {
            enabled = false
        }
    }
}
