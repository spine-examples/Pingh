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

import com.google.protobuf.Duration
import com.google.protobuf.util.Durations
import io.spine.base.Time.currentTime
import io.spine.core.External
import io.spine.examples.pingh.clock.event.TimePassed
import io.spine.examples.pingh.github.PersonalAccessToken
import io.spine.examples.pingh.sessions.command.LogUserIn
import io.spine.examples.pingh.sessions.command.LogUserOut
import io.spine.examples.pingh.sessions.command.UpdateToken
import io.spine.examples.pingh.sessions.command.VerifySession
import io.spine.examples.pingh.sessions.command.VerifyUserLoginToGitHub
import io.spine.examples.pingh.sessions.event.SessionClosed
import io.spine.examples.pingh.sessions.event.SessionExpired
import io.spine.examples.pingh.sessions.event.SessionVerificationFailed
import io.spine.examples.pingh.sessions.event.SessionVerified
import io.spine.examples.pingh.sessions.event.TokenUpdated
import io.spine.examples.pingh.sessions.event.UserCodeReceived
import io.spine.examples.pingh.sessions.event.UserIsNotLoggedIntoGitHub
import io.spine.examples.pingh.sessions.event.UserLoggedIn
import io.spine.examples.pingh.sessions.event.UserLoggedOut
import io.spine.examples.pingh.sessions.rejection.NotMemberOfPermittedOrgs
import io.spine.examples.pingh.sessions.rejection.Rejections
import io.spine.examples.pingh.sessions.rejection.SessionAlreadyClosed
import io.spine.examples.pingh.sessions.rejection.UsernameMismatch
import io.spine.logging.Logging
import io.spine.protobuf.Durations2.minutes
import io.spine.server.command.Assign
import io.spine.server.event.React
import io.spine.server.model.Nothing
import io.spine.server.procman.ProcessManager
import io.spine.server.tuple.EitherOf2
import io.spine.server.tuple.EitherOf3
import kotlin.jvm.Throws

/**
 * Coordinates session management, that is, user login and logout.
 */
@Suppress("TooManyFunctions" /* Managing sessions requires numerous functions. */)
internal class UserSessionProcess :
    ProcessManager<SessionId, UserSession, UserSession.Builder>(), Logging {

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
        _debug().log("${command.id.forLog()}: Starting login process.")
        val codes = auth.requestVerificationCodes()
        _debug().log("${command.id.forLog()}: Verification codes received.")
        val loginTime = min(codes.expiresIn, maxLoginTime)
        with(builder()) {
            deviceCode = codes.deviceCode
            loginDeadline = currentTime().add(loginTime)
        }
        return UserCodeReceived::class.buildWith(
            command.id,
            codes.userCode,
            codes.verificationUrl,
            loginTime,
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
     * @throws NotMemberOfPermittedOrgs if the user is not a member of any permitted organizations.
     */
    @Assign
    @Throws(UsernameMismatch::class, NotMemberOfPermittedOrgs::class)
    internal fun handle(
        command: VerifyUserLoginToGitHub
    ): EitherOf2<UserLoggedIn, UserIsNotLoggedIntoGitHub> {
        _debug().log("${command.id.forLog()}: Trying to verify GitHub login.")
        val tokens = try {
            auth.requestAccessToken(state().deviceCode)
        } catch (exception: CannotObtainAccessToken) {
            _debug().log(
                "${command.id.forLog()}: GitHub login verification failed. " +
                        "GitHub responded with the message: \"${exception.errorName}\"."
            )
            return EitherOf2.withB(UserIsNotLoggedIntoGitHub::class.withSession(command.id))
        }
        _debug().log(
            "${command.id.forLog()}: GitHub issued an access token. " +
                    "Checking if the user has permission to access the application."
        )
        ensureUsernameMatching(tokens.accessToken)
        ensureMembershipInPermittedOrgs(tokens.accessToken)
        _debug().log("${command.id.forLog()}: GitHub login verified and login process finished.")
        with(builder()) {
            refreshToken = tokens.refreshToken
            whenExpires = currentTime().add(lifetime)
            clearDeviceCode()
            clearLoginDeadline()
        }
        return EitherOf2.withA(
            UserLoggedIn::class.with(command.id, tokens.accessToken, tokens.whenExpires)
        )
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
        val expectedUsername = state().id.username
        val loggedInUser = users.ownerOf(token)
        if (!expectedUsername.equals(loggedInUser.username)) {
            throw UsernameMismatch::class.with(state().id, expectedUsername, loggedInUser.username)
        }
    }

    /**
     * Throws an `NotMemberOfPermittedOrgs` rejection if the user is not a member
     * of any [permitted organizations][PermittedOrganizations].
     */
    @Throws(NotMemberOfPermittedOrgs::class)
    private fun ensureMembershipInPermittedOrgs(token: PersonalAccessToken) {
        val userOrganizations = users.memberships(token)
        if (!userOrganizations.any { PermittedOrganizations.contains(it) }) {
            throw NotMemberOfPermittedOrgs::class.with(state().id)
        }
    }

    /**
     * Renews GitHub access tokens using the refresh token.
     *
     * @throws SessionAlreadyClosed if current session was already closed.
     */
    @Assign
    @Throws(SessionAlreadyClosed::class)
    internal fun handle(command: UpdateToken): TokenUpdated {
        if (!isActive || !state().hasRefreshToken()) {
            _warn().log(
                "${id().forLog()}: Token update was requested, " +
                        "but the session is already closed, " +
                        "resulting in the update being rejected."
            )
            throw SessionAlreadyClosed.newBuilder().setId(command.id).build()
        }
        _debug().log("${state().id.forLog()}: Refreshing the access token.")
        val tokens = auth.refreshAccessToken(state().refreshToken)
        _debug().log("${state().id.forLog()}: GitHub issued a new access token.")
        builder().setRefreshToken(tokens.refreshToken)
        return TokenUpdated::class.with(
            command.id, tokens.accessToken, tokens.whenExpires
        )
    }

    /**
     * Emits event when a user logs out.
     */
    @Assign
    internal fun handle(command: LogUserOut): UserLoggedOut {
        deleted = true
        _debug().log("${state().id.forLog()}: Session closed because the user logged out.")
        return UserLoggedOut::class.with(command.id)
    }

    /**
     * Closes the user session if a `UsernameMismatch` rejection is emitted
     * during the login process.
     *
     * A `UsernameMismatch` rejection is a critical exception
     * that prevents the login process from being completed.
     */
    @React
    internal fun on(rejection: Rejections.UsernameMismatch): SessionClosed {
        deleted = true
        _debug().log(
            "${state().id.forLog()}: Login failed and session closed because " +
                    "a token issued for \"${rejection.loggedInUser.value}\" account " +
                    "but \"${rejection.expectedUser.value}\" username was entered."
        )
        return SessionClosed::class.with(rejection.id)
    }

    /**
     * Closes the user session if a `NotMemberOfPermittedOrgs` rejection is emitted
     * during the login process.
     *
     * A `NotMemberOfPermittedOrgs` rejection is a critical exception
     * that prevents the login process from being completed.
     */
    @React
    internal fun on(rejection: Rejections.NotMemberOfPermittedOrgs): SessionClosed {
        _debug().log(
            "${state().id.forLog()}: Login failed and session closed because " +
                    "user is not a member of an organization authorized to use the application."
        )
        deleted = true
        return SessionClosed::class.with(rejection.id)
    }

    /**
     * Closes the session if the login is not completed and the specified time has passed,
     * or if the session is active but has exceeded its expiration time.
     */
    @React
    internal fun on(
        @External event: TimePassed
    ): EitherOf3<SessionClosed, SessionExpired, Nothing> =
        when {
            !isActive -> EitherOf3.withC(nothing())

            state().hasLoginDeadline() && state().loginDeadline <= event.time -> {
                deleted = true
                _debug().log(
                    "${state().id.forLog()}: Login failed and session closed " +
                            "because the time to complete login expired."
                )
                EitherOf3.withA(SessionClosed::class.with(state().id))
            }

            state().hasWhenExpires() && state().whenExpires <= event.time -> {
                deleted = true
                _debug().log("${state().id.forLog()}: Session expired, so it closed.")
                EitherOf3.withB(SessionExpired::class.with(state().id))
            }

            else -> EitherOf3.withC(nothing())
        }

    /**
     * Verifies whether the session is active.
     *
     * If the session is active, `SessionVerified` event is emitted;
     * if it is inactive, `SessionVerificationFailed` event is emitted.
     */
    @Assign
    internal fun handle(
        command: VerifySession
    ): EitherOf2<SessionVerified, SessionVerificationFailed> =
        if (isActive && state().hasRefreshToken()) {
            EitherOf2.withA(SessionVerified::class.with(command.id))
        } else {
            EitherOf2.withB(SessionVerificationFailed::class.with(command.id))
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

    internal companion object {
        /**
         * Maximum duration of the login process.
         */
        internal val maxLoginTime = minutes(3)

        /**
         * The duration for which a session remains active after its creation.
         */
        internal val lifetime = Durations.fromDays(30)
    }
}

/**
 * Returns the minimum duration.
 */
private fun min(d1: Duration, d2: Duration): Duration =
    if (Durations.toNanos(d1) < Durations.toNanos(d2)) d1 else d2
