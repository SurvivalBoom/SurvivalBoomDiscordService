plugins {
    java
}

group = "net.survivalboom.sbds.modules.changestatus"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":api"))
}