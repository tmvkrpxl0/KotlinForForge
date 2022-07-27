import java.time.LocalDateTime

val kotlin_version: String by project
val annotations_version: String by project
val coroutines_version: String by project
val serialization_version: String by project

val mc_version: String by project
val forge_version: String by project

plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("net.minecraftforge.gradle")
    id("com.github.johnrengelman.shadow") version "7.1.2"
    `maven-publish`
}

java {
    withSourcesJar()
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

// Workaround to remove build\java from MOD_CLASSES because SJH doesn't like nonexistent dirs
setOf(sourceSets.main, sourceSets.test)
    .map(Provider<SourceSet>::get)
    .forEach { sourceSet ->
        val mutClassesDirs = sourceSet.output.classesDirs as ConfigurableFileCollection
        val javaClassDir = sourceSet.java.classesDirectory.get()
        val mutClassesFrom = mutClassesDirs.from
            .filter {
                val toCompare = (it as? Provider<*>)?.get()
                return@filter javaClassDir != toCompare
            }
            .toMutableSet()
        mutClassesDirs.setFrom(mutClassesFrom)
    }

val library: Configuration by configurations.creating {
    exclude("org.jetbrains", "annotations")
}

configurations {
    api {
        extendsFrom(library)
    }
}

repositories {
    mavenCentral()
    // For testing with kfflib and making JarJar shut up
    mavenLocal()
}

dependencies {
    minecraft("net.minecraftforge:forge:$mc_version-$forge_version")

    library(kotlin("reflect"))
    library(kotlin("stdlib-jdk8"))
    library("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")
    library("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$coroutines_version")
    library("org.jetbrains.kotlinx:kotlinx-serialization-json:$serialization_version")
}

minecraft {
    mappings("official", "1.19")

    runs {
        create("client") {
            workingDirectory(project.file("run"))

            property("forge.logging.markers", "SCAN,LOADING,CORE")
            property("forge.logging.console.level", "debug")

            mods {
                create("kotlinforforge") {
                    source(sourceSets.main.get())
                }

                create("kfflangtest") {
                    source(sourceSets.test.get())
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

                create("kfflangtest") {
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
    jar {
        archiveClassifier.set("slim")
    }
   
    shadowJar {
        configurations = listOf(library)
        archiveClassifier.set("")

        dependencies {
            include(dependency("org.jetbrains.kotlin:kotlin-stdlib:${kotlin_version}"))
            include(dependency("org.jetbrains.kotlin:kotlin-stdlib-jdk7:${kotlin_version}"))
            include(dependency("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${kotlin_version}"))
            include(dependency("org.jetbrains.kotlin:kotlin-reflect:${kotlin_version}"))
            include(dependency("org.jetbrains:annotations:${annotations_version}"))
            include(dependency("org.jetbrains.kotlinx:kotlinx-coroutines-core:${coroutines_version}"))
            include(dependency("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:${coroutines_version}"))
            include(dependency("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${coroutines_version}"))
            include(dependency("org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:${serialization_version}"))
            include(dependency("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:${serialization_version}"))
        }
    }
    
    withType<Jar> {
        manifest.attributes(
            "FMLModType" to "LANGPROVIDER",
            "Specification-Title" to "Kotlin for Forge Language Provider",
            "Automatic-Module-Name" to "kfflang",
            "Specification-Vendor" to "Forge",
            "Specification-Version" to "1",
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to "thedarkcolour",
            "Implementation-Timestamp" to LocalDateTime.now()
        )
    }
    
    assemble {
        dependsOn(shadowJar)
    }
    
    // Only require the lang provider to use explicit visibility modifiers, not the test mod
    compileKotlin {
        kotlinOptions.freeCompilerArgs = listOf("-Xexplicit-api=warning", "-Xjvm-default=all")
    }
}

afterEvaluate {
    val javaComponent = components["java"] as AdhocComponentWithVariants
    javaComponent.withVariantsFromConfiguration(configurations.runtimeElements.get(), ConfigurationVariantDetails::skip)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "kotlinforforge"
            from(components["java"])
        }
    }
}
