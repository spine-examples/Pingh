plugins {
    java
}

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
