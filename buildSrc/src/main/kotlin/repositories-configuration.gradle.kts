plugins {
    java
}

/**
 * Configures repositories for access to dependencies, including
 * Spine Event Engine.
 */
repositories {
    mavenLocal()
    gradlePluginPortal()
    mavenCentral()

    maven {
        url = uri("https://spine.mycloudrepo.io/public/repositories/releases")
        mavenContent {
            releasesOnly()
        }
    }
}
