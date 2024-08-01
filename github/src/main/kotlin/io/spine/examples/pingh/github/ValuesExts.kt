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

package io.spine.examples.pingh.github

import com.google.protobuf.util.Timestamps
import io.spine.base.Time.currentTime
import io.spine.examples.pingh.github.rest.AccessTokenFragment
import io.spine.examples.pingh.github.rest.AccessTokenResponse
import io.spine.examples.pingh.github.rest.CommentFragment
import io.spine.examples.pingh.github.rest.IssueOrPullRequestFragment
import io.spine.examples.pingh.github.rest.VerificationCodesFragment
import io.spine.examples.pingh.github.rest.VerificationCodesResponse
import io.spine.net.Url
import io.spine.protobuf.Durations2.seconds
import kotlin.reflect.KClass

/**
 * Creates a new [Username] with the specified string value.
 */
public fun KClass<Username>.buildBy(value: String): Username =
    Username.newBuilder()
        .setValue(value)
        .vBuild()

/**
 * Returns the GitHub tag of the user.
 *
 * The tag consists of the `'@'` character followed by the `Username`.
 */
public fun Username.tag(): String = "@${this.value}"

/**
 * Creates a new [PersonalAccessToken] with the specified string value.
 */
public fun KClass<PersonalAccessToken>.buildBy(value: String): PersonalAccessToken =
    PersonalAccessToken.newBuilder()
        .setValue(value)
        .vBuild()

/**
 * Creates a new [Url] with the specified string value.
 */
public fun KClass<Url>.buildBy(spec: String): Url =
    Url.newBuilder()
        .setSpec(spec)
        .vBuild()

/**
 * Creates a new [User] with the specified [Username] and avatar [Url].
 */
public fun KClass<User>.buildBy(username: Username, avatarUrl: Url): User =
    User.newBuilder()
        .setUsername(username)
        .setAvatarUrl(avatarUrl)
        .vBuild()

/**
 * Creates a new [User] with the specified username and avatar URL.
 */
public fun KClass<User>.buildBy(username: String, avatarUrl: String): User =
    this.buildBy(
        Username::class.buildBy(username),
        Url::class.buildBy(avatarUrl)
    )

/**
 * Creates a new `Mention` with the data specified in the `IssueOrPullRequestFragment`.
 */
public fun KClass<Mention>.buildFromFragment(fragment: IssueOrPullRequestFragment): Mention =
    with(Mention.newBuilder()) {
        id = NodeId::class.buildBy(fragment.nodeId)
        author = User::class.buildBy(
            fragment.whoCreated.username,
            fragment.whoCreated.avatarUrl
        )
        title = fragment.title
        whenMentioned = Timestamps.parse(fragment.whenCreated)
        url = Url::class.buildBy(fragment.htmlUrl)
        vBuild()
    }

/**
 * Creates a new `Mention` with the passed title value and
 * the data specified in the `CommentFragment`.
 *
 * Comments do not have titles, which are required to create a `Mention`s.
 * Therefore, it is necessary to additionally specify which value is considered
 * a mention's title. It is recommended to use the GitHub title of the item
 * under which the comment is made.
 */
public fun KClass<Mention>.buildFromFragment(
    fragment: CommentFragment,
    itemTitle: String
): Mention =
    with(Mention.newBuilder()) {
        id = NodeId::class.buildBy(fragment.nodeId)
        author = User::class.buildBy(
            fragment.whoCreated.username,
            fragment.whoCreated.avatarUrl
        )
        title = itemTitle
        whenMentioned = Timestamps.parse(fragment.whenCreated)
        url = Url::class.buildBy(fragment.htmlUrl)
        vBuild()
    }

/**
 * Creates a new `DeviceCode` with the specified string value.
 */
public fun KClass<DeviceCode>.buildBy(value: String): DeviceCode =
    DeviceCode.newBuilder()
        .setValue(value)
        .vBuild()

/**
 * Creates a new `UserCode` with the specified string value.
 */
public fun KClass<UserCode>.buildBy(value: String): UserCode =
    UserCode.newBuilder()
        .setValue(value)
        .vBuild()

/**
 * Creates a new `RefreshToken` with the specified string value.
 */
public fun KClass<RefreshToken>.buildBy(value: String): RefreshToken =
    RefreshToken.newBuilder()
        .setValue(value)
        .vBuild()

/**
 * Creates a new `VerificationCodesResponse` with the data specified
 * in the `VerificationCodesResponse`.
 */
public fun KClass<VerificationCodesResponse>.buildFromFragment(
    fragment: VerificationCodesFragment
): VerificationCodesResponse =
    with(VerificationCodesResponse.newBuilder()) {
        deviceCode = DeviceCode::class.buildBy(fragment.deviceCode)
        userCode = UserCode::class.buildBy(fragment.userCode)
        verificationUrl = Url::class.buildBy(fragment.verificationUri)
        expiresIn = seconds(fragment.expiresIn)
        interval = seconds(fragment.interval)
        vBuild()
    }

/**
 * Creates a new `AccessTokenResponse` with the data specified
 * in the `AccessTokenFragment`.
 */
public fun KClass<AccessTokenResponse>.buildFromFragment(
    fragment: AccessTokenFragment
): AccessTokenResponse =
    with(AccessTokenResponse.newBuilder()) {
        accessToken = PersonalAccessToken::class.buildBy(fragment.accessToken)
        whenExpires = Timestamps.add(currentTime(), seconds(fragment.expiresIn))
        refreshToken = RefreshToken::class.buildBy(fragment.refreshToken)
        vBuild()
    }
