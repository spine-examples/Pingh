import io.spine.internal.gradle.standardToSpineSdk

plugins {
    kotlin("jvm")
}

allprojects {
    apply(from = "$rootDir/version.gradle.kts")
    version = extra["pinghVersion"]!!
    group = "io.spine.examples"

    repositories.standardToSpineSdk()
}

subprojects {
    apply {
        plugin("kotlin")
    }
}
