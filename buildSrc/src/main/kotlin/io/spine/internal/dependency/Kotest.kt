package io.spine.internal.dependency

/**
 * Testing framework for Kotlin.
 *
 * @see <a href="https://kotest.io/">Kotest site</a>
 */
@Suppress("unused", "ConstPropertyName")
public object Kotest {
    private const val version = "5.8.0"
    private const val group = "io.kotest"
    // TODO:2024.05.14:MykytaPimonovTD: Delete unused dependencies
    public const val assertions: String = "$group:kotest-assertions-core:$version"
    public const val runnerJUnit5: String = "$group:kotest-runner-junit5:$version"
    public const val runnerJUnit5Jvm: String = "$group:kotest-runner-junit5-jvm:$version"
    public const val frameworkApi: String = "$group:kotest-framework-api:$version"
    public const val datatest: String = "$group:kotest-framework-datatest:$version"
    public const val frameworkEngine: String = "$group:kotest-framework-engine:$version"
}
