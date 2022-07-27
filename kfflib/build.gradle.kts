import java.time.LocalDateTime

val coroutines_version: String by project
val serialization_version: String by project

val mc_version: String by project
val forge_version: String by project

plugins {
    kotlin("jvm")
    `maven-publish`
    id("net.minecraftforge.gradle")
}

java {
    withSourcesJar()
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

repositories {
    mavenCentral()

    mavenLocal()
}

val library: Configuration by configurations.creating {
    exclude("org.jetbrains", "annotations")
}

configurations {
    api {
        extendsFrom(library)
    }
    
    runtimeElements {
        exclude(group = "net.minecraftforge", module = "forge")
    }
}

dependencies {
    minecraft("net.minecraftforge:forge:$mc_version-$forge_version")
    
    library(kotlin("stdlib-jdk8"))
    library(kotlin("reflect"))
    library("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")
    library("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:$coroutines_version")
    library("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$coroutines_version")
    library("org.jetbrains.kotlinx:kotlinx-serialization-json:$serialization_version")

    implementation(group = "thedarkcolour", name = "kotlinforforge", version = "[${project.version}, 4.0)")
}

configurations.all {
    resolutionStrategy.dependencySubstitution {
        substitute(module("thedarkcolour:kotlinforforge"))
            .using(project(":kfflang"))
            .because("Include from local instead of maven")
    }
}

minecraft {
    mappings("official", "1.19")

    runs {
        create("client") {
            workingDirectory(project.file("run"))

            property("forge.logging.markers", "SCAN,LOADING,CORE")
            property("forge.logging.console.level", "debug")

            mods {
                create("kfflib") {
                    source(sourceSets.main.get())
                }
                create("kfflibtest") {
                    source(sourceSets.test.get())
                }
            }
        }


        create("server") {
            workingDirectory(project.file("run/server"))

            property("forge.logging.markers", "SCAN,LOADING,CORE")
            property("forge.logging.console.level", "debug")

            mods {
                create("kfflib") {
                    source(sourceSets.main.get())
                }
                create("kfflibtest") {
                    source(sourceSets.test.get())
                }
            }
        }

        all {
            lazyToken("minecraft_classpath") {
                library.copyRecursive().resolve()
                    .joinToString(separator = File.pathSeparator, transform = File::getAbsolutePath)
            }
        }
    }
}

tasks {
    withType<Jar> {
        manifest.attributes(
            "FMLModType" to "GAMELIBRARY",
            "Specification-Title" to "kfflib",
            "Automatic-Module-Name" to "kfflib",
            "Specification-Vendor" to "Forge",
            "Specification-Version" to "1",
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to "thedarkcolour",
            "Implementation-Timestamp" to LocalDateTime.now()
        )
    }
    
    // Only require the lang provider to use explicit visibility modifiers, not the test mod
    compileKotlin {
        kotlinOptions.freeCompilerArgs = listOf("-Xexplicit-api=warning", "-Xjvm-default=all")
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
