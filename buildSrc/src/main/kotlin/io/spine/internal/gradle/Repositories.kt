package io.spine.internal.gradle

import org.gradle.api.artifacts.dsl.RepositoryHandler

/**
 * Applies repositories commonly used by Kotlin projects.
 */
fun RepositoryHandler.standardRepositories() {
    mavenCentral()
    gradlePluginPortal()
}
