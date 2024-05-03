import io.spine.internal.gradle.standardRepositories
import io.spine.internal.standardRepositories

buildscript {
    standardRepositories()
}

allprojects {
    repositories.standardRepositories()
}