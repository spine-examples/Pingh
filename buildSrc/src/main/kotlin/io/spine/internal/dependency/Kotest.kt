package io.spine.internal.dependency

/**
 * Testing framework for Kotlin.
 *
 * @see <a href="https://kotest.io/">Kotest site</a>
 */
@Suppress("unused")
public object Kotest {
    private const val version = "5.8.0"
    public const val assertions: String = "io.kotest:kotest-assertions-core:$version"
}
