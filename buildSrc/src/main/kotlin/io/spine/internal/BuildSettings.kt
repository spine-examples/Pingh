package io.spine.internal

import org.gradle.jvm.toolchain.JavaLanguageVersion

/**
 * This object provides high-level constants, like version of JVM, to be used
 * throughout the project.
 */
object BuildSettings {

    private const val JVM_VERSION = 17
    val javaVersion: JavaLanguageVersion = JavaLanguageVersion.of(JVM_VERSION)
}
