plugins {
    `java-library`
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnlyApi(project(":api"))
}