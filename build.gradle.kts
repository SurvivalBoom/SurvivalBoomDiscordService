import java.nio.file.Files

group = "net.survivalboom.survivalboomapi"
version = "3.0.0"

val outFile = File(childProjects["core"]!!.layout.buildDirectory.asFile.orNull, "libs/SBDS-${version}.jar")
val runDir = File(rootProject.projectDir, "run")
val runFile = File(runDir, outFile.name)
val runModules = File(runDir, "modules")

tasks {

    val copyToRun = create("copyToRun") {

        dependsOn(":core:shadowJar")

        doFirst {

            runDir.mkdirs()

            Files.deleteIfExists(runFile.toPath())
            Files.copy(outFile.toPath(), runFile.toPath())

        }

    }

    val copyModulesToRun = create("copyModulesToRun") {

        dependsOn(copyToRun)

        val projects = subprojects.stream().filter { project ->
            File(project.projectDir, "src/main/resources/module.yml").exists()
        }.toList()

        projects.forEach { project -> dependsOn("${project.name}:build") }

      doLast {

            runModules.mkdirs()

            projects.forEach { project ->

                val jarTask = project.tasks.named("jar", Jar::class)
                val moduleFile = jarTask.get().archiveFile.get().asFile
                val newModuleFile = File(runModules, moduleFile.name)

                Files.deleteIfExists(newModuleFile.toPath())
                Files.copy(moduleFile.toPath(), newModuleFile.toPath())

            }

        }

    }

    create<Exec>("runApp") {

        dependsOn(copyModulesToRun)
        workingDir = runDir
        commandLine("java", "-jar", runFile.name)

    }

   create("clean") {

        doLast {
            Files.deleteIfExists(runFile.toPath())
        }

    }


}
