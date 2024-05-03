import io.spine.internal.gradle.standardRepositories

plugins {
    kotlin("jvm")
}

allprojects {
    apply(from = "$rootDir/version.gradle.kts")
    version = extra["pinghVersion"]!!
    group = "io.spine.examples"

    repositories.standardRepositories()
}

subprojects {
    apply {
        plugin("kotlin")
    }
}
