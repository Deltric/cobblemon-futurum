import utilities.VersionType
import utilities.writeVersion

plugins {
    id("cobblemon.base-conventions")
    id("com.gradleup.shadow")
}

writeVersion(type = VersionType.FULL)

val bundle: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

loom {
    val clientConfig = runConfigs.getByName("client")
    clientConfig.runDir = "runClient"
//    clientConfig.vmArg("-Dmixin.debug=true")
    clientConfig.programArg("--username=AshKetchum")
    //This is AshKetchum's UUID so you get an Ash Ketchum skin
    clientConfig.programArg("--uuid=93e4e551-589a-41cb-ab2d-435266c8e035")
    val serverConfig = runConfigs.getByName("server")
    serverConfig.runDir = "runServer"

    // Forge established the "main" name convention we're using here. Since NeoForged already defines it we must use
    // `maybeCreate`
    mods.maybeCreate("main")
    // This configurations ensures that the code and resources in :common are available to the platform-specific builds
    // in specifically the development mode. This code does nothing for the final packed jar since that one uses shadow
    // bundling / jar-in-jar magic to make sure the platform-specific jar has all relevant files available.
    mods.named("main") {
        sourceSet(project.sourceSets.main.get())
        sourceSet(project(":common").sourceSets.main.get())
    }

    // The shaded archive is the distribution. Nest dependencies on that task so
    // merged services, relocated classes and Multi-Release manifests survive.
    nestJars(tasks.shadowJar, configurations.named("include"))
}

tasks {

    jar {
        archiveBaseName.set("Cobblemon-${project.name}")
        archiveClassifier.set("dev-slim")
    }

    shadowJar {
        archiveClassifier.set("")
        archiveBaseName.set("Cobblemon-${project.name}")
        archiveVersion.set("${rootProject.version}")
        configurations = listOf(bundle)
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        filesMatching("META-INF/services/**") {
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
        mergeServiceFiles()
        manifest.attributes("Fabric-Mapping-Namespace" to "official")

    }

    val copyJar by registering(CopyFile::class) {
        val productionJar = tasks.shadowJar.flatMap { it.archiveFile }
        fileToCopy = productionJar
        destination = productionJar.flatMap {
            rootProject.layout.buildDirectory.file("libs/${it.asFile.name}")
        }
    }

    assemble {
        dependsOn(copyJar)
    }

}
