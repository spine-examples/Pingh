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

package io.spine.examples.pingh.server

import com.google.cloud.secretmanager.v1.SecretManagerServiceClient
import com.google.cloud.secretmanager.v1.SecretVersionName

/**
 * Allows to access application secrets stored
 * in [Google Cloud Secret Manager](https://cloud.google.com/secret-manager).
 */
internal object Secret {
    /**
     * Retrieves the secret with the specified [name][secretId].
     *
     * The latest version of the secret available in the current [project][projectId] is retrieved.
     *
     * To retrieve the secret, the [project ID][projectId] must be provided as
     * a system parameter with the key `GCP_PROJECT_ID`.
     */
    internal fun named(secretId: String): String =
        SecretManagerServiceClient.create().use { client ->
            val version = SecretVersionName.of(projectId(), secretId, "latest")
            client.accessSecretVersion(version)
                .payload
                .data
                .toStringUtf8()
        }

    /**
     * Returns the Google Cloud Platform project ID from system properties.
     *
     * @throws IllegalStateException if `GCP_PROJECT_ID` property is empty.
     */
    private fun projectId(): String =
        System.getProperty("GCP_PROJECT_ID") ?: throw IllegalStateException(
            "The project ID is not specified as a system property using the `GCP_PROJECT_ID` key."
        )
}
