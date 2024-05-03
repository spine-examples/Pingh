package io.spine.internal.gradle.kotlin

import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/**
 * Sets [Java toolchain](https://kotlinlang.org/docs/gradle.html#gradle-java-toolchains-support)
 * to the specified version.
 */
fun KotlinJvmProjectExtension.applyJvmToolchain(version: Int) {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(version))
    }
}
