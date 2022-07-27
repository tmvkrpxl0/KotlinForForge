import net.minecraftforge.gradle.userdev.tasks.JarJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("net.minecraftforge.gradle") version "5.1.+"
}

val mc_version: String by project
val forge_version: String by project

val coroutines_version: String by project
val serialization_version: String by project

// Current KFF version
val kffVersion = "3.7.0"
val kffGroup = "thedarkcolour"

allprojects {
    version = kffVersion
    group = kffGroup
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(17))
jarJar.enable()

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
}

minecraft {
    mappings("official", mc_version)

    runs {
        create("client") {
            workingDirectory(project.file("run"))

            property("forge.logging.markers", "SCAN,LOADING,CORE")
            property("forge.logging.console.level", "debug")

            mods {
                create("kotlinforforge") {
                    source(sourceSets.main.get())
                }
            }
        }

        create("server") {
            workingDirectory(project.file("run/server"))

            property("forge.logging.markers", "SCAN,LOADING,CORE")
            property("forge.logging.console.level", "debug")

            mods {
                create("kotlinforforge") {
                    source(sourceSets.main.get())
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

dependencies {
    minecraft("net.minecraftforge:forge:$mc_version-$forge_version")

    library(kotlin("reflect"))
    library(kotlin("stdlib-jdk8"))
    library("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")
    library("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$coroutines_version")
    library("org.jetbrains.kotlinx:kotlinx-serialization-json:$serialization_version")

    implementation("thedarkcolour", "kotlinforforge", "[${project.version}, 4.0)")
    implementation("thedarkcolour", "kfflib", "[${project.version}, 4.0)")
    include(project(":kfflang"))
    include(project(":kfflib"))
}

configurations.all {
    resolutionStrategy.dependencySubstitution {
        substitute(module("thedarkcolour:kfflib")).using(project(":kfflib"))
        substitute(module("thedarkcolour:kotlinforforge")).using(project(":kfflang"))
    }
}

fun DependencyHandlerScope.include(dependency: ModuleDependency) {
    jarJar(dependency) {
        isTransitive = false
        jarJar.pin(this, dependency.version)
    }
}

tasks {
    // Sets final jar name to match old name
    named<JarJar>("jarJar") {
        archiveBaseName.set("kotlinforforge")
        archiveClassifier.set("obf")
    }
    
    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "17"
        }
    }
}
