import java.nio.file.Files

group = "net.survivalboom.sbds"
version = "4.0.0"

val outFile = File(childProjects["core"]!!.layout.buildDirectory.asFile.orNull, "libs/SBDS-${version}.jar")
val runDir = File(rootProject.projectDir, "run")
val runFile = File(runDir, outFile.name)
val runModules = File(runDir, "modules")

subprojects {

    plugins.matching { it is JavaPlugin }.configureEach {
        configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(21))
            }
        }

        tasks.named<ProcessResources>("processResources") {
            filesMatching("module.yml") {
                duplicatesStrategy = DuplicatesStrategy.INCLUDE
                expand(mapOf("version" to project.version))
            }
        }
    }

    afterEvaluate {

        val resourcesDir = file("src/main/resources")
        val hasModuleYml = !fileTree(resourcesDir).matching {
            include("**/module.yml")
        }.isEmpty

        if (hasModuleYml) {

            val copyModuleToRun by tasks.registering(Copy::class) {

                val archiveProvider = provider {
                    tasks.findByName("shadowJar") ?: tasks.getByName("jar")
                }

                dependsOn(archiveProvider)

                from(archiveProvider.flatMap { (it as AbstractArchiveTask).archiveFile })

                into(runModules)

            }

        }

    }

}

tasks {

    val copyToRun = create("copyToRun") {

        dependsOn(":core:shadowJar")

        doFirst {

            runDir.mkdirs()

            Files.deleteIfExists(runFile.toPath())
            Files.copy(outFile.toPath(), runFile.toPath())

        }

    }

    create<Exec>("runApp") {

        dependsOn(copyToRun)

        workingDir = runDir
        commandLine("java", "-jar", runFile.name)

    }

   create("cleanRun") {

        doLast {
            Files.deleteIfExists(runFile.toPath())
        }

    }

}
