allprojects {

    apply(from = "$rootDir/version.gradle.kts")
    version = extra["pinghVersion"]!!
    group = "io.spine.examples"

    apply<IdeaPlugin>()
}

subprojects {

    apply<DependencyManagementPlugin>()
}
