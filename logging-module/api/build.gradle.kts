plugins {
    java
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":api"))
}