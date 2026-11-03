/*
 *
 *  * Copyright (C) 2023 Cobblemon Contributors
 *  *
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 */

plugins {
    id("cobblemon.platform-conventions")
    id("cobblemon.publish-conventions")
}

architectury {
    platformSetupLoomIde()
    neoForge()
}

loom {
    neoForge {

    }
}

repositories {
    maven(url = "${rootProject.projectDir}/deps")
    maven(url = "https://thedarkcolour.github.io/KotlinForForge/")
    maven(url = "https://api.modrinth.com/maven")
    maven(url = "https://maven.neoforged.net/releases")
    mavenLocal()
}

dependencies {
    neoForge(libs.neoforge.api)
    //shadowCommon group: 'commons-io', name: 'commons-io', version: '2.6'
    //implementation(libs.flywheelForge)
    //include(libs.flywheelForge)
    // DebugUtils has no 26.3 release; do not load the old development-only mod.
    runtimeOnly(libs.bundles.neoforge.integrations.runtimeOnly)
    runtimeOnly(libs.bundles.mongo)
    forgeRuntimeLibrary(libs.bundles.mongo)

    implementation(libs.bundles.kotlin)
    include(libs.kotlin.stdlib)
    include(libs.kotlin.reflect)
    forgeRuntimeLibrary(libs.bundles.kotlin)

    implementation(project(":common")) {
        isTransitive = false
    }
    bundle(project(path = ":common", configuration = "transformProductionNeoForge")) {
        isTransitive = false
    }
    testImplementation(project(":common"))

    listOf(
        libs.bundles.mongo,
        libs.bundles.graal
    ).forEach {
        include(it)
    }
    runtimeOnly(libs.bundles.graal)
    forgeRuntimeLibrary(libs.bundles.graal)

    listOf(
        libs.molang
    ).forEach {
        forgeRuntimeLibrary(it)
        bundle(it)
    }

}

tasks {
    shadowJar {
        exclude("architectury-common.accessWidener")
        exclude("architectury.common.json")

    }

    processResources {
        inputs.property("version", rootProject.version)
        inputs.property("minecraft_version", rootProject.property("mc_version").toString())
        inputs.property("java_version", rootProject.property("java_version").toString())
        inputs.property("neoforge_version", libs.versions.neoforge.get())

        filesMatching("META-INF/neoforge.mods.toml") {
            expand(
                "version" to rootProject.version,
                "minecraft_version" to rootProject.property("mc_version").toString(),
                "java_version" to rootProject.property("java_version").toString(),
                "neoforge_version" to libs.versions.neoforge.get()
            )
        }
    }
}

tasks {
    sourcesJar {
        val depSources = project(":common").tasks.sourcesJar
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        dependsOn(depSources)
        from(depSources.get().archiveFile.map { zipTree(it) }) {
            exclude("architectury.accessWidener")
        }
    }
}

// Unobfuscated releases transform the production jar directly.
loom.neoForge {
    convertAccessWideners(tasks.shadowJar, "cobblemon-common.accesswidener")
}


//jar {
//    classifier("dev")
//    manifest {
//        attributes(
//                "Specification-Title" to rootProject.mod_id,
//                "Specification-Vendor" to "Cable MC",
//                "Specification-Version" to "1",
//                "Implementation-Title" to rootProject.mod_id,
//                "Implementation-Version" to project.version,
//                "Implementation-Vendor" to "Cable MC",
//        )
//    }
//}
//
//sourcesJar {
//    def commonSources = project(":common").sourcesJar
//    dependsOn commonSources
//    from commonSources.archiveFile.map { zipTree(it) }
//}
//
//components.java {
//    withVariantsFromConfiguration(project.configurations.shadowRuntimeElements) {
//        skip()
//    }
//}
