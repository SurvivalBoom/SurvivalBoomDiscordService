plugins {
    java
    id("com.gradleup.shadow") version "8.3.0"
}

group = "net.survivalboom.sbds.modules.logging"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":api"))

    implementation(project(":logging-module:api"))
}

tasks {
    named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
        archiveFileName.set("${project.parent?.name}-${project.version}.jar")
    }

    named("build") {
        dependsOn("shadowJar")
    }
}