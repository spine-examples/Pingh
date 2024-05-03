import io.spine.internal.gradle.standardRepositories
import io.spine.internal.standardRepositories

buildscript {
    standardRepositories()
}

plugins {
    kotlin("jvm")
}

allprojects {
    repositories.standardRepositories()
}