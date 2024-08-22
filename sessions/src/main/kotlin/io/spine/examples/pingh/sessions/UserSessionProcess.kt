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

package io.spine.examples.pingh.sessions

import io.spine.examples.pingh.sessions.command.LogUserIn
import io.spine.examples.pingh.sessions.command.LogUserOut
import io.spine.examples.pingh.sessions.command.VerifyUserLoginToGitHub
import io.spine.examples.pingh.sessions.event.UserCodeReceived
import io.spine.examples.pingh.sessions.event.UserIsNotLoggedIntoGitHub
import io.spine.examples.pingh.sessions.event.UserLoggedIn
import io.spine.examples.pingh.sessions.event.UserLoggedOut
import io.spine.server.command.Assign
import io.spine.server.procman.ProcessManager
import io.spine.server.tuple.EitherOf2

/**
 * Coordinates session management, that is, user login and logout.
 */
internal class UserSessionProcess :
    ProcessManager<SessionId, UserSession, UserSession.Builder>() {

    /**
     * Service for generating access tokens via GitHub.
     *
     * It is expected this field is set by calling [inject]
     * right after the instance creation.
     */
    private lateinit var authenticationService: GitHubAuthentication

    /**
     * Requests user and device codes from GitHub for authentication.
     *
     * The device code is stored in the entity, while the user code is included
     * in the emitted `UserCodeReceived` event. This event also provides information
     * on where to enter the code and the timeout duration between entry attempts.
     */
    @Assign
    internal fun handle(command: LogUserIn): UserCodeReceived {
        val codes = authenticationService.requestVerificationCodes()
        with(builder()) {
            deviceCode = codes.deviceCode
        }
        return UserCodeReceived::class.buildWith(
            command.id,
            codes.userCode,
            codes.verificationUrl,
            codes.expiresIn,
            codes.interval
        )
    }

    /**
     * Requests access tokens from GitHub using the device code.
     *
     * If the user has already entered the user code issued with the device code, the login
     * is considered successful, and the `UserLoggedIn` event is emitted. Otherwise,
     * the `UserIsNotLoggedIntoGitHub` event is emitted.
     */
    @Assign
    internal fun handle(
        command: VerifyUserLoginToGitHub
    ): EitherOf2<UserLoggedIn, UserIsNotLoggedIntoGitHub> {
        val tokens = try {
            authenticationService.requestAccessToken(state().deviceCode)
        } catch (exception: CannotObtainAccessToken) {
            return EitherOf2.withB(UserIsNotLoggedIntoGitHub::class.withSession(command.id))
        }
        with(builder()) {
            refreshToken = tokens.refreshToken
            whenAccessTokenExpires = tokens.whenExpires
            clearDeviceCode()
        }
        return EitherOf2.withA(UserLoggedIn::class.buildBy(command.id, tokens.accessToken))
    }

    /**
     * Emits event when a user logs out.
     */
    @Assign
    internal fun handle(command: LogUserOut): UserLoggedOut {
        deleted = true
        return UserLoggedOut::class.buildBy(command.id)
    }

    /**
     * Supplies this instance of the process with a service for generating verification codes
     * and access tokens via GitHub.
     *
     * It is expected this method is called right after the creation of the process instance.
     * Otherwise, the process will not be able to function properly.
     */
    internal fun inject(authenticationService: GitHubAuthentication) {
        this.authenticationService = authenticationService
    }
}
