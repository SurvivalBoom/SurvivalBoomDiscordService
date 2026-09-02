import java.io.ByteArrayOutputStream

plugins {
    application
    id("net.kyori.blossom") version "2.1.0"
    id("com.gradleup.shadow") version "9.4.1"
}

group = rootProject.group;
version = rootProject.version;

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":api"))
    compileOnly("ch.qos.logback:logback-classic:1.5.6") // logging
}

application {
    mainClass = "net.survivalboom.sbds.core.Main"
}



tasks {

    shadowJar {

        archiveBaseName = "SBDS"
        archiveVersion = rootProject.version.toString()
        archiveClassifier = ""

    }

    build {
        dependsOn(shadowJar)
    }

}


sourceSets {
    main {
        blossom {
            javaSources {
                property("version", project.version.toString())
                property("commit", getGitCommitHash())
                property("compiled_by", getBuildUser())
            }
        }
    }
}

fun getGitCommitHash(): String {
    return try {

        val process = ProcessBuilder("git", "rev-parse", "--short", "HEAD")
            .directory(project.rootDir)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()

        val output = process.inputStream.bufferedReader().readText().trim()
        if (process.waitFor() == 0 && output.isNotEmpty()) output else "NO-GIT"
    } catch (e: Exception) {
        "NO-GIT"
    }
}

fun getBuildUser(): String {
    val ciUser = System.getenv("GITHUB_ACTOR") // GitHub Actions
        ?: System.getenv("GITLAB_USER_LOGIN")  // GitLab CI
        ?: System.getenv("BUILD_USER")         // Jenkins

    return ciUser ?: System.getProperty("user.name") ?: "UNKNOWN"
}
