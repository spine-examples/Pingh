package io.spine.internal

import org.gradle.kotlin.dsl.ScriptHandlerScope
import io.spine.internal.gradle.standardRepositories

/**
 * Applies [standard][standardRepositories] repositories to this `buildscript`.
 */
fun ScriptHandlerScope.standardRepositories() {
    repositories.standardRepositories()
}