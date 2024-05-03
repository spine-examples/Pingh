import io.spine.internal.dependency.Pingh
import io.spine.internal.gradle.standardRepositories

plugins {
    kotlin("jvm")
}

allprojects {
    apply(from = "$rootDir/version.gradle.kts")
    version = extra["pinghVersion"]!!
    group = Pingh.group

    repositories.standardRepositories()
}

subprojects {
    apply {
        plugin("kotlin")
    }
}