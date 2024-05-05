/**
 * Gradle configuration for the whole project.
 */
allprojects {
    /*
    * Import the `version.gradle.kts` file and set the version and group for each module.
    */
    apply(from = "$rootDir/version.gradle.kts")
    version = extra["pinghVersion"]!!
    group = "io.spine.examples"

    apply<IdeaPlugin>()
}

/**
 * The configuration is divided in multiple script plugins located in `buildSrc/src/main/kotlin`.
 * Each of these plugins contains a more detailed description in their source file.
 */
subprojects {
    apply<JavaPlugin>()

    /*
     * Configure repositories.
     */
    apply<RepositoriesConfigurationPlugin>()
}
