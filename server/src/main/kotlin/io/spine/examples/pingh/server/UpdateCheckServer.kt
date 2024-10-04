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

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.spine.examples.pingh.clock.Clock

/**
 * The port for listening to HTTP requests.
 */
private const val updateCheckPort = 8080

/**
 * Starts the server that handles requests to check for updates.
 *
 * The server runs in the background on port [updateCheckPort].
 *
 * This server does not perform any updates itself; it simply processes the incoming
 * HTTP requests and forwards a message to the [Pingh server][PinghApplication.server],
 * which is responsible for executing the updates.
 */
internal fun startUpdateCheckServer(clock: Clock) {
    embeddedServer(
        Netty,
        port = updateCheckPort
    ) {
        configure(clock)
    }.start(wait = false)
}

/**
 * Handles HTTP requests.
 *
 * Emits an event with the current time upon receiving an update check request
 * and returning a `200 OK` status in response.
 */
private fun Application.configure(clock: Clock) {
    routing {
        post("/check-for-updates") {
            clock.triggerTimePassed()
            call.respond(HttpStatusCode.OK)
        }
    }
}
