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

@file:Suppress("UnusedReceiverParameter" /* Class extensions don't use class as a parameter. */)

package io.spine.examples.pingh.github

import com.google.protobuf.Timestamp
import com.google.protobuf.util.Timestamps
import io.spine.base.Time.currentTime
import io.spine.examples.pingh.github.rest.AccessTokenFragment
import io.spine.examples.pingh.github.rest.AccessTokenResponse
import io.spine.examples.pingh.github.rest.CommentFragment
import io.spine.examples.pingh.github.rest.IssueOrPullRequestFragment
import io.spine.examples.pingh.github.rest.OrganizationFragment
import io.spine.examples.pingh.github.rest.ReviewFragment
import io.spine.examples.pingh.github.rest.UserFragment
import io.spine.examples.pingh.github.rest.VerificationCodesFragment
import io.spine.examples.pingh.github.rest.VerificationCodesResponse
import io.spine.net.Url
import io.spine.protobuf.Durations2.seconds
import kotlin.reflect.KClass

/**
 * Creates a new `Username` with the specified string value.
 */
public fun KClass<Username>.of(value: String): Username =
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
 * Creates a new `PersonalAccessToken` with the specified string value.
 */
public fun KClass<PersonalAccessToken>.of(value: String): PersonalAccessToken =
    PersonalAccessToken.newBuilder()
        .setValue(value)
        .vBuild()

/**
 * Creates a new `Url` with the specified string value.
 */
public fun KClass<Url>.of(spec: String): Url =
    Url.newBuilder()
        .setSpec(spec)
        .vBuild()

/**
 * Creates a new `User` with the specified `Username` and avatar `Url`.
 */
public fun KClass<User>.of(username: Username, avatarUrl: Url): User =
    User.newBuilder()
        .setUsername(username)
        .setAvatarUrl(avatarUrl)
        .vBuild()

/**
 * Creates a new `User` with the specified username and avatar URL.
 */
public fun KClass<User>.of(username: String, avatarUrl: String): User =
    of(
        Username::class.of(username),
        Url::class.of(avatarUrl)
    )

/**
 * Creates a new `Repo` with the passed name and owner.
 */
public fun KClass<Repo>.of(owner: String, name: String): Repo =
    Repo.newBuilder()
        .setName(name)
        .setOwner(owner)
        .vBuild()

/**
 * Returns the `Repo` where this issue or pull request was created.
 */
public fun IssueOrPullRequestFragment.repo(): Repo = repoFrom(htmlUrl)

/**
 * Parses a URL of an HTML GitHub item within the repository.
 *
 * Repository items are e.g. issues, pull requests, comments on issues,
 * reviews, review comments, etc.
 */
private fun repoFrom(htmlUrl: String): Repo {
    val pathSegments = htmlUrl.substring("https://".length).split("/")
    return Repo::class.of(pathSegments[1], pathSegments[2])
}

/**
 * Creates a new `Mention` with the data specified in the `IssueOrPullRequestFragment`.
 */
public fun KClass<Mention>.from(fragment: IssueOrPullRequestFragment): Mention =
    with(Mention.newBuilder()) {
        id = NodeId::class.of(fragment.nodeId)
        author = User::class.of(
            fragment.whoCreated.username,
            fragment.whoCreated.avatarUrl
        )
        title = fragment.title
        whenMentioned = Timestamp::class.parse(fragment.whenCreated)
        url = Url::class.of(fragment.htmlUrl)
        whereMentioned = fragment.repo()
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
public fun KClass<Mention>.from(fragment: CommentFragment, itemTitle: String): Mention =
    with(Mention.newBuilder()) {
        id = NodeId::class.of(fragment.nodeId)
        author = User::class.of(
            fragment.whoCreated.username,
            fragment.whoCreated.avatarUrl
        )
        title = "Comment on $itemTitle"
        whenMentioned = Timestamp::class.parse(fragment.whenCreated)
        url = Url::class.of(fragment.htmlUrl)
        whereMentioned = repoFrom(fragment.htmlUrl)
        vBuild()
    }

/**
 * Creates a new `Mention` with the passed title value and
 * the data specified in the `ReviewFragment`.
 *
 * Reviews do not have titles, which are required to create a `Mention`s.
 * Therefore, it is necessary to additionally specify which value is considered
 * a mention's title. It is recommended to use the title of the pull request for
 * which the review is submitted.
 */
public fun KClass<Mention>.from(fragment: ReviewFragment, prTitle: String): Mention =
    with(Mention.newBuilder()) {
        id = NodeId::class.of(fragment.nodeId)
        author = User::class.of(
            fragment.whoCreated.username,
            fragment.whoCreated.avatarUrl
        )
        title = "Review of $prTitle"
        whenMentioned = Timestamp::class.parse(fragment.whenSubmitted)
        url = Url::class.of(fragment.htmlUrl)
        whereMentioned = repoFrom(fragment.htmlUrl)
        vBuild()
    }

/**
 * Creates a new `DeviceCode` with the specified string value.
 */
public fun KClass<DeviceCode>.of(value: String): DeviceCode =
    DeviceCode.newBuilder()
        .setValue(value)
        .vBuild()

/**
 * Creates a new `UserCode` with the specified string value.
 */
public fun KClass<UserCode>.of(value: String): UserCode =
    UserCode.newBuilder()
        .setValue(value)
        .vBuild()

/**
 * Creates a new `RefreshToken` with the specified string value.
 */
public fun KClass<RefreshToken>.of(value: String): RefreshToken =
    RefreshToken.newBuilder()
        .setValue(value)
        .vBuild()

/**
 * Creates a new `VerificationCodesResponse` with the data specified
 * in the `VerificationCodesResponse`.
 */
public fun KClass<VerificationCodesResponse>.fromFragment(
    fragment: VerificationCodesFragment
): VerificationCodesResponse =
    with(VerificationCodesResponse.newBuilder()) {
        deviceCode = DeviceCode::class.of(fragment.deviceCode)
        userCode = UserCode::class.of(fragment.userCode)
        verificationUrl = Url::class.of(fragment.verificationUri)
        expiresIn = seconds(fragment.expiresIn)
        interval = seconds(fragment.interval)
        vBuild()
    }

/**
 * Creates a new `AccessTokenResponse` with the data specified
 * in the `AccessTokenFragment`.
 */
public fun KClass<AccessTokenResponse>.fromFragment(
    fragment: AccessTokenFragment
): AccessTokenResponse =
    with(AccessTokenResponse.newBuilder()) {
        accessToken = PersonalAccessToken::class.of(fragment.accessToken)
        whenExpires = Timestamps.add(currentTime(), seconds(fragment.expiresIn))
        refreshToken = RefreshToken::class.of(fragment.refreshToken)
        vBuild()
    }

/**
 * Creates a new `ClientSecret` with the specified string value.
 */
public fun KClass<ClientSecret>.of(value: String): ClientSecret =
    ClientSecret.newBuilder()
        .setValue(value)
        .vBuild()

/**
 * Creates a new `User` with the data specified in the `UserFragment`.
 */
public fun KClass<User>.from(fragment: UserFragment): User =
    this.of(fragment.username, fragment.avatarUrl)

/**
 * Creates a new `OrganizationLogin` with the passed string value.
 */
public fun KClass<OrganizationLogin>.of(value: String): OrganizationLogin =
    OrganizationLogin.newBuilder()
        .setValue(value)
        .vBuild()

/**
 * Creates a new `Organization` with the data specified in the `OrganizationFragment`.
 */
public fun KClass<Organization>.from(fragment: OrganizationFragment): Organization =
    Organization.newBuilder()
        .setLogin(OrganizationLogin::class.of(fragment.login))
        .vBuild()

/**
 * Creates a new `Organization` with the passed login value.
 */
public fun KClass<Organization>.loggedAs(login: String): Organization =
    Organization.newBuilder()
        .setLogin(OrganizationLogin::class.of(login))
        .vBuild()

/**
 * Creates a new `GitHubApp` with the passed GitHub App client ID and secret.
 */
public fun KClass<GitHubApp>.of(id: ClientId, secret: ClientSecret): GitHubApp =
    GitHubApp.newBuilder()
        .setId(id)
        .setSecret(secret)
        .vBuild()
