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

package io.spine.examples.pingh.testing.mentions.given

import com.google.protobuf.Timestamp
import io.ktor.http.HttpStatusCode
import io.spine.examples.pingh.github.Mention
import io.spine.examples.pingh.github.PersonalAccessToken
import io.spine.examples.pingh.github.Team
import io.spine.examples.pingh.github.Username
import io.spine.examples.pingh.mentions.CannotObtainMentionsException
import io.spine.examples.pingh.mentions.GitHubSearch
import java.lang.Thread.sleep
import kotlin.jvm.Throws

/**
 * Implementation of [GitHubSearch] that fetches mentions
 * from a JSON file in the resource folder.
 *
 * Uses exclusively for testing.
 */
public class PredefinedGitHubSearchResponses : GitHubSearch {

    public companion object {
        /**
         * The time after which the process will try to run again if it is frozen.
         * The value is specified in milliseconds.
         */
        private const val timeBetweenExecutionAttempts = 100L
    }

    /**
     * Indicates whether to freeze the execution of the [searchMentions] method.
     *
     * If `true`, the method will be executed indefinitely, if `false`,
     * it will terminate without problems. The value can be changed during execution.
     */
    private var isFrozen = false

    /**
     * The HTTP status response code from GitHub.
     *
     * If the value differs from `200 OK`, an [CannotObtainMentionsException] is thrown.
     */
    private var responseStatusCode = HttpStatusCode.OK

    /**
     * Indicates whether mentions from this service have been successfully obtained.
     *
     * Once mentions are successfully fetched, this value is set to `true`. To reset the value,
     * use the [mentionsAreNotFetched] method.
     *
     * This variable helps prevent duplicate mentions when [fetching][searchMentions] them again.
     */
    private var areUserMentionsFetched = false

    /**
     * Whether team mentions from this service have been successfully obtained.
     *
     * Once mentions are successfully fetched, this value is set to `true`. To reset the value,
     * use the [mentionsAreNotFetched] method.
     *
     * This variable helps prevent duplicate mentions when [fetching][searchMentions] them again.
     */
    private var areTeamMentionsFetched = false

    /**
     * Returns set of [Mention]s retrieved from a JSON file in the resource folder,
     * or empty set if mentions have already been fetched.
     */
    @Throws(CannotObtainMentionsException::class)
    public override fun searchMentions(
        username: Username,
        token: PersonalAccessToken,
        updatedAfter: Timestamp,
        limit: Int?
    ): Set<Mention> {
        if (responseStatusCode != HttpStatusCode.OK) {
            throw CannotObtainMentionsException(responseStatusCode.value)
        }
        if (areUserMentionsFetched) {
            return emptySet()
        }
        val mentions = userMentions()
        waitWhileServiceIsFrozen()
        areUserMentionsFetched = true
        return mentions
    }

    /**
     * Returns set of team [Mention]s retrieved from a JSON file in the resource folder,
     * or empty set if mentions have already been fetched.
     */
    @Throws(CannotObtainMentionsException::class)
    override fun searchMentions(
        team: Team,
        token: PersonalAccessToken,
        updatedAfter: Timestamp,
        limit: Int?
    ): Set<Mention> {
        if (areTeamMentionsFetched) {
            return emptySet()
        }
        val mentions = teamMentions()
        waitWhileServiceIsFrozen()
        areTeamMentionsFetched = true
        return mentions
    }

    /**
     * Waits for the service to be unfrozen.
     */
    private fun waitWhileServiceIsFrozen() {
        while (isFrozen) {
            sleep(timeBetweenExecutionAttempts)
        }
    }

    /**
     * Marks the service as frozen.
     *
     * The process of fetching mentions from GitHub will not be completed until the service
     * is unfrozen by calling the [unfreeze] method.
     */
    public fun freeze() {
        isFrozen = true
    }

    /**
     * Marks the service as unfrozen, i.e. the process of fetching mentions
     * from GitHub can be completed.
     */
    public fun unfreeze() {
        isFrozen = false
    }

    /**
     * Sets the HTTP status of the response.
     *
     * If the status code is different from `200 OK`, an exception will be thrown.
     * The [setDefaultResponseStatusCode] method can be used to return the response status code
     * to the default value.
     */
    public fun setResponseStatusCode(responseStatusCode: HttpStatusCode) {
        this.responseStatusCode = responseStatusCode
    }

    /**
     * Sets the HTTP response status to `200 OK`.
     */
    public fun setDefaultResponseStatusCode() {
        responseStatusCode = HttpStatusCode.OK
    }

    /**
     * Indicates that mentions have not yet been fetched.
     */
    public fun mentionsAreNotFetched() {
        areUserMentionsFetched = false
        areTeamMentionsFetched = false
    }

    /**
     * Resets the instance to its initial state.
     *
     * Once the instance is reset, it is no longer frozen
     * and allows mentions to be retrieved again.
     */
    public fun reset() {
        unfreeze()
        setDefaultResponseStatusCode()
        mentionsAreNotFetched()
    }
}
