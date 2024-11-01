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

import java.util.Properties

/**
 * Pingh server endpoint with the necessary connection details.
 *
 * @property address The address of the Pingh server.
 * @property port The port on which the Pingh server is running.
 */
internal data class ServerEndpoint(
    val address: String,
    val port: Int
) {
    internal companion object {
        /**
         * Loads server endpoint properties from resource folder.
         */
        internal fun load(): ServerEndpoint {
            val properties = Properties()
            val path = "/config/server.properties"
            ServerEndpoint::class.java.getResourceAsStream(path).use {
                properties.load(it)
            }
            val errorMessageFormat = "To connect to the Pingh server, the \"%s\" property " +
                    "must be specified in the configuration file at \"resource$path\"."
            return ServerEndpoint(
                properties.getOrThrow("server.address", errorMessageFormat),
                properties.getOrThrow("server.port", errorMessageFormat).toInt()
            )
        }
    }
}

/**
 * Returns the value of property by its `key` if it exists;
 * otherwise, an [IllegalStateException] is thrown.
 */
private fun Properties.getOrThrow(key: String, errorMessageFormat: String): String =
    getProperty(key) ?: throw IllegalStateException(errorMessageFormat.format(key))
