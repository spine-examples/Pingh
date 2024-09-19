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

import io.spine.core.External
import io.spine.examples.pingh.clock.event.TimePassed
import io.spine.examples.pingh.github.Organization
import io.spine.examples.pingh.github.PersonalAccessToken
import io.spine.examples.pingh.github.loggedAs
import io.spine.examples.pingh.sessions.command.LogUserIn
import io.spine.examples.pingh.sessions.command.LogUserOut
import io.spine.examples.pingh.sessions.command.RefreshToken
import io.spine.examples.pingh.sessions.command.VerifyUserLoginToGitHub
import io.spine.examples.pingh.sessions.event.TokenRefreshed
import io.spine.examples.pingh.sessions.event.UserCodeReceived
import io.spine.examples.pingh.sessions.event.UserIsNotLoggedIntoGitHub
import io.spine.examples.pingh.sessions.event.UserLoggedIn
import io.spine.examples.pingh.sessions.event.UserLoggedOut
import io.spine.examples.pingh.sessions.rejection.OrgAccessDenied
import io.spine.examples.pingh.sessions.rejection.UsernameMismatch
import io.spine.server.command.Assign
import io.spine.server.command.Command
import io.spine.server.procman.ProcessManager
import io.spine.server.tuple.EitherOf2
import java.util.Optional
import kotlin.jvm.Throws

/**
 * Organizations whose members are allowed to authorize with the Pingh app.
 *
 * For this to work correctly, the organization must have
 * the [Pingh application](https://github.com/apps/pingh-tracker-of-github-mentions)
 * installed on GitHub.
 */
internal val permittedOrganizations: Set<Organization> = setOf(
    Organization::class.loggedAs("SpineEventEngine"),
    Organization::class.loggedAs("TeamDev-Ltd"),
    Organization::class.loggedAs("TeamDev-IP")
)

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
    private lateinit var auth: GitHubAuthentication

    /**
     * Service for obtaining user's information via GitHub.
     *
     * It is expected this field is set by calling [inject]
     * right after the instance creation.
     */
    private lateinit var users: GitHubUsers

    /**
     * Requests user and device codes from GitHub for authentication.
     *
     * The device code is stored in the entity, while the user code is included
     * in the emitted `UserCodeReceived` event. This event also provides information
     * on where to enter the code and the timeout duration between entry attempts.
     */
    @Assign
    internal fun handle(command: LogUserIn): UserCodeReceived {
        val codes = auth.requestVerificationCodes()
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
     *
     * @throws UsernameMismatch if the username of the logged-in account differs from
     *   the one provided at the start.
     * @throws OrgAccessDenied if the user is not a member of any permitted organizations.
     */
    @Assign
    @Throws(UsernameMismatch::class, OrgAccessDenied::class)
    internal fun handle(
        command: VerifyUserLoginToGitHub
    ): EitherOf2<UserLoggedIn, UserIsNotLoggedIntoGitHub> {
        val tokens = try {
            auth.requestAccessToken(state().deviceCode)
        } catch (exception: CannotObtainAccessToken) {
            return EitherOf2.withB(UserIsNotLoggedIntoGitHub::class.withSession(command.id))
        }
        ensureUsernameMatching(tokens.accessToken)
        ensureThatOrgHaveAccess(tokens.accessToken)
        with(builder()) {
            refreshToken = tokens.refreshToken
            whenAccessTokenExpires = tokens.whenExpires
            clearDeviceCode()
        }
        return EitherOf2.withA(UserLoggedIn::class.buildBy(command.id, tokens.accessToken))
    }

    /**
     * Throws an `UsernameMismatch` rejection if the username
     * of the logged-in account differs from the one entered at the start.
     *
     * GitHub's device flow for authentication does not use usernames,
     * but the initial username is required to start a user session.
     * Therefore, it's essential to verify that the entered username matches
     * the account from which the user authenticated.
     */
    @Throws(UsernameMismatch::class)
    private fun ensureUsernameMatching(token: PersonalAccessToken) {
        val loggedInUser = users.ownerOf(token)
        if (!loggedInUser.username.equals(state().id.username)) {
            throw UsernameMismatch::class.with(state().id, loggedInUser.username)
        }
    }

    /**
     * Throws an `OrgAccessDenied` rejection if the user is not a member
     * of any [permitted organizations][permittedOrganizations].
     */
    @Throws(OrgAccessDenied::class)
    private fun ensureThatOrgHaveAccess(token: PersonalAccessToken) {
        val userOrganizations = users.memberships(token)
        if (!userOrganizations.any { permittedOrganizations.contains(it) }) {
            throw OrgAccessDenied::class.with(state().id, permittedOrganizations.toList())
        }
    }

    /**
     * Sends a `RefreshToken` command if the user is logged in
     * and the personal access token is expired.
     */
    @Command
    internal fun on(@External event: TimePassed): Optional<RefreshToken> {
        val isUserLoggedIn = isActive && state().hasRefreshToken()
        return if (isUserLoggedIn && event.time >= state().whenAccessTokenExpires) {
            Optional.of(RefreshToken::class.with(state().id, event.time))
        } else {
            Optional.empty()
        }
    }

    /**
     * Renews GitHub access tokens using the refresh token.
     */
    @Assign
    internal fun handle(command: RefreshToken): TokenRefreshed {
        val tokens = auth.refreshAccessToken(state().refreshToken)
        with(builder()) {
            whenAccessTokenExpires = tokens.whenExpires
            refreshToken = tokens.refreshToken
        }
        return TokenRefreshed::class.with(command.id, tokens.accessToken, command.whenRequested)
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
     * Supplies this instance with a service for generating GitHub verification codes
     * and access tokens, as well as a service for retrieving user information from GitHub.
     *
     * It is expected this method is called right after the creation of the process instance.
     * Otherwise, the process will not be able to function properly.
     *
     * @param auth The service that allows to access GitHub authentication API.
     * @param users The service that allows to retrieve user information using the GitHub API.
     */
    internal fun inject(auth: GitHubAuthentication, users: GitHubUsers) {
        this.auth = auth
        this.users = users
    }
}
