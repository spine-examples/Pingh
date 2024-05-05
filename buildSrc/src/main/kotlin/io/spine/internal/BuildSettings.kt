package io.spine.internal

import org.gradle.jvm.toolchain.JavaLanguageVersion

/**
 * This object provides high-level constants, like version of JVM, to be used
 * throughout the project.
 *
 * It cannot be used in the build script of `buildSrc` itself.
 */
public object BuildSettings {

    private const val JVM_VERSION = 17
    public val javaVersion: JavaLanguageVersion = JavaLanguageVersion.of(JVM_VERSION)
}
