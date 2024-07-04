plugins {
    kotlin("jvm") version "1.9.20"

    application
}

group = "io.spine.examples"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(11)
}

application {
    mainClass.set("io.spine.examples.pingh.desktop.MainKt")
}
