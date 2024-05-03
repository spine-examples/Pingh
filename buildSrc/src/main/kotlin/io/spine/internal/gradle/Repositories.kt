package io.spine.internal.gradle

import org.gradle.api.artifacts.dsl.RepositoryHandler

/**
 * Applies repositories commonly used by kotlin projects.
 */
fun RepositoryHandler.standardRepositories() {
    mavenCentral()
    gradlePluginPortal()
}
