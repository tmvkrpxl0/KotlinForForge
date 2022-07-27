import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.minecraftforge.gradle.patcher.tasks.ReobfuscateJar

val kotlin_version: String by project
val annotations_version: String by project
val coroutines_version: String by project
val serialization_version: String by project
val max_kotlin: String by project
val max_coroutines: String by project
val max_serialization: String by project

val mc_version: String by project
val forge_version: String by project

plugins {
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("net.minecraftforge.gradle")
    `maven-publish`
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(17))
kotlin.jvmToolchain {}

val kotlinSourceJar by tasks.creating(Jar::class) {
    val kotlinSourceSet = kotlin.sourceSets.main.get()

    from(kotlinSourceSet.kotlin.srcDirs)
    archiveClassifier.set("sources")
}

tasks.build.get().dependsOn(kotlinSourceJar)

// Workaround to remove build\java from MOD_CLASSES because SJH doesn't like nonexistent dirs
for (s in arrayOf(sourceSets.main, sourceSets.test)) {
    val sourceSet = s.get()
    val mutClassesDirs = sourceSet.output.classesDirs as ConfigurableFileCollection
    val javaClassDir = sourceSet.java.classesDirectory.get()
    val mutClassesFrom = HashSet(mutClassesDirs.from.filter {
        val provider = it as Provider<*>?
        val toCompare = if (it != null) provider!!.get() else it
        return@filter javaClassDir != toCompare
    })
    mutClassesDirs.setFrom(mutClassesFrom)
}

configurations {
    val library = maybeCreate("library")
    api.configure {
        extendsFrom(library)
    }
}

minecraft.runs.all {
    lazyToken("minecraft_classpath") {
        return@lazyToken configurations["library"].copyRecursive().resolve()
            .joinToString(File.pathSeparator) { it.absolutePath }
    }
}

repositories {
    mavenCentral()
    // For testing with kfflib and making JarJar shut up
    mavenLocal()
}

dependencies {

    val l = configurations["library"]
    fun library(dependency: String) {
        l(dependency) {
            exclude("org.jetbrains", "annotations")
        }
    }

    minecraft("net.minecraftforge:forge:$mc_version-$forge_version")

    library("org.jetbrains.kotlin:kotlin-reflect:$kotlin_version")
    library("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")
    library("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")
    library("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$coroutines_version")
    library("org.jetbrains.kotlinx:kotlinx-serialization-json:$serialization_version")
}

minecraft.run {
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
    }
}

tasks.withType<Jar> {
    archiveBaseName.set("kfflang")
    archiveClassifier.set("slim")

    manifest {
        attributes(
            mapOf(
                "FMLModType" to "LANGPROVIDER",
                "Specification-Title" to "Kotlin for Forge Language Provider",
                "Automatic-Module-Name" to "kfflang",
                "Specification-Vendor" to "Forge",
                "Specification-Version" to "1",
                "Implementation-Title" to project.name,
                "Implementation-Version" to "${project.version}",
                "Implementation-Vendor" to "thedarkcolour",
                "Implementation-Timestamp" to `java.text`.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
                    .format(`java.util`.Date())
            )
        )
    }
}

val shadowJar = tasks.withType<ShadowJar> {
    configurations = listOf(project.configurations["library"])
    archiveClassifier.set("")
    // finalizedBy("reobfShadowJar")

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

tasks.assemble.get().dependsOn(shadowJar)

tasks.withType<ReobfuscateJar> {
    shadowJar
}

// Only require the lang provider to use explicit visibility modifiers, not the test mod
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().getByName("compileKotlin") {
    kotlinOptions.freeCompilerArgs = listOf("-Xexplicit-api=warning", "-Xjvm-default=all")
}

fun DependencyHandler.minecraft(
    dependencyNotation: Any
): Dependency? = add("minecraft", dependencyNotation)

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "kotlinforforge"
            from(components["kotlin"])
            artifact(project.tasks.shadowJar) {
                classifier = ""
            }
            artifact(kotlinSourceJar) {
                classifier = "sources"
            }

            fg.component(this)
        }
    }
}
