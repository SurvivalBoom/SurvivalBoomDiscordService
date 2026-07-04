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
    implementation(project(":logging-module:api"))
}

tasks {

    shadowJar {
        archiveFileName = "${parent?.name}-${version}.jar"
    }

    build {
        dependsOn(shadowJar)
    }

}