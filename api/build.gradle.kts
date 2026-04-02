plugins {
    `java-library`
    java
    `maven-publish`
}

group = "net.survivalboom.sbds" // Какого то хуя оно не хочет пихатся без этого
version = rootProject.version

repositories {
    mavenCentral()
}

dependencies {

    api("net.dv8tion:JDA:5.2.2")
    api("org.bspfsystems:yamlconfiguration:2.0.1")
    api("org.hibernate.orm:hibernate-core:6.6.9.Final")
    api("org.json:json:20240303")
    api("org.jetbrains:annotations:15.0")

    api("com.fasterxml.jackson.core:jackson-core:2.18.3")
    api("com.fasterxml.jackson.core:jackson-databind:2.18.3")

}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        register<MavenPublication>("api") {
            from(components["java"])
            artifactId = "api"
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/SurvivalBoom/SurvivalBoomDiscordService")
            credentials {
                username = System.getenv("GH_PACKAGES_USER")
                password = System.getenv("GH_PACKAGES_TOKEN")
            }
        }
    }
}