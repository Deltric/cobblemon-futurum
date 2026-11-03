/*
 *
 *  * Copyright (C) 2023 Cobblemon Contributors
 *  *
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

configurations.all {
    resolutionStrategy {
        force(libs.fabric.loader)
    }
}

plugins {
    id("cobblemon.platform-conventions")
    id("cobblemon.publish-conventions")
}

architectury {
    platformSetupLoomIde()
    fabric()
}

val generatedResources = file("src/generated/resources")

sourceSets.main {
    resources {
        srcDir(generatedResources)
    }
}

repositories {
    maven(url = "${rootProject.projectDir}/deps")
    mavenLocal()
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven(url = "https://api.modrinth.com/maven")
    maven(url = "https://maven.terraformersmc.com/")
}

dependencies {
    implementation(project(":common")) {
        isTransitive = false
    }
    bundle(project(path = ":common", configuration = "transformProductionFabric")) {
        isTransitive = false
    }
    // DebugUtils has no 26.3 release; do not load the old development-only mod.
    implementation(libs.fabric.loader)
    api(libs.fabric.api)
    api(libs.bundles.fabric)

    compileOnly(libs.bundles.common.integrations.compileOnly) {
        isTransitive = false
    }

    implementation(libs.bundles.fabric.integrations.implementation)
    runtimeOnly(libs.bundles.fabric.integrations.runtimeOnly)
    runtimeOnly(libs.bundles.mongo)

//    implementation(libs.flywheelFabric)
//    include(libs.flywheelFabric)

    listOf(
        libs.fabric.kotlin,
        libs.fabric.permissions,
        libs.bundles.graal,
        libs.bundles.mongo
    ).forEach {
        include(it)
    }

    // Added to make graal available in dev
    runtimeOnly(libs.bundles.graal)

    include(libs.fabric.kotlin)

    listOf(
        libs.molang
    ).forEach {
        bundle(it)
        runtimeOnly(it)
    }

}

publishing {
    publications.named<MavenPublication>(project.name) {
        pom.withXml {
            val root = asNode()
            val depsNode = (root["dependencies"] as? groovy.util.NodeList)
                ?.firstOrNull() as? groovy.util.Node
                ?: root.appendNode("dependencies")

            configurations.getByName("include").allDependencies
                .filterIsInstance<ExternalModuleDependency>()
                .forEach { dep ->
                    depsNode.appendNode("dependency").apply {
                        appendNode("groupId", dep.group)
                        appendNode("artifactId", dep.name)
                        appendNode("version", dep.version)
                        appendNode("scope", "compile")
                    }
                }
        }
    }
}

tasks {
    // The AW file is needed in :fabric project resources when the game is run.
    val copyAccessWidener by registering(Copy::class) {
        from(loom.accessWidenerPath)
        into(generatedResources)
        dependsOn(checkLicenseMain)
    }

    processResources {
        dependsOn(copyAccessWidener)
        inputs.property("version", rootProject.version)
        inputs.property("fabric_loader_version", libs.versions.fabric.loader.get())
        inputs.property("fabric_api_version", libs.versions.fabric.api.get())
        inputs.property("minecraft_version", rootProject.property("mc_version").toString())
        inputs.property("java_version", rootProject.property("java_version").toString())

        filesMatching("fabric.mod.json") {
            expand(
                "version" to rootProject.version,
                "fabric_loader_version" to libs.versions.fabric.loader.get(),
                "fabric_api_version" to libs.versions.fabric.api.get(),
                "minecraft_version" to rootProject.property("mc_version").toString(),
                "java_version" to rootProject.property("java_version").toString()
            )
        }
    }

    sourcesJar {
        dependsOn(copyAccessWidener)
        val commonSources = project(":common").tasks.named<Jar>("sourcesJar")
        dependsOn(commonSources)
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        from(commonSources.flatMap { it.archiveFile }.map { zipTree(it) }) {
            exclude("architectury.common.json")
        }
    }
}
