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

package io.spine.internal.dependency

/**
 * A library for loading images from the Internet asynchronously.
 *
 * @see <a href="https://github.com/coil-kt/coil">Coil GitHub</a>
 * @see <a href="https://coil-kt.github.io/coil/upgrading_to_coil3/#multiplatform">
 *     Coil with Compose Multiplatform</a>
 */
public object Coil {
    // Compose Multiplatform requires Coil version 3.x for compatibility,
    // but it is currently in the alpha phase.
    // TODO:2024-07-17:mykyta.pimonov: Bump this version from 3.0.0-alpha08 to 3.0.0
    //  upon its release.
    //  See: https://github.com/spine-examples/Pingh/issues/25.
    private const val version = "3.0.0-alpha08"
    private const val group = "io.coil-kt.coil3"

    public const val lib: String = "$group:coil:$version"
    public const val networkKtor: String = "$group:coil-network-ktor:$version"
    public const val compose: String = "$group:coil-compose:$version"
}
