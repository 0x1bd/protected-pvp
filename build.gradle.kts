import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.2.20"
    id("net.neoforged.moddev") version "2.0.148"
    id("maven-publish")
}

version = project.property("mod_version") as String
group = project.property("maven_group") as String

base {
    archivesName.set(project.property("archives_base_name") as String)
}

val targetJavaVersion = 21
java {
    toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    withSourcesJar()
}

repositories {
    mavenCentral()
    maven("https://cursemaven.com") {
        content { includeGroup("curse.maven") }
    }
    maven("https://thedarkcolour.github.io/KotlinForForge/") {
        name = "Kotlin for Forge"
        content { includeGroup("thedarkcolour") }
    }
}

neoForge {
    version = project.property("neo_version") as String
    runs {
        create("server") {
            server()
            gameDirectory = file("run/neoforge-1.21.1")
            programArgument("--nogui")
        }
    }
    mods {
        create(project.property("mod_id") as String) {
            sourceSet(sourceSets.main.get())
        }
    }
    unitTest {
        enable()
        testedMod = mods.getByName(project.property("mod_id") as String)
    }
}

dependencies {
    implementation("thedarkcolour:kotlinforforge-neoforge:${project.property("kotlin_for_forge_version")}")
    compileOnly("curse.maven:irons_spells_n_spellbooks-855414:8237097")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
    testImplementation("org.mockito:mockito-core:5.14.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

tasks.named<JavaExec>("runServer") {
    standardInput = System.`in`
}

tasks.processResources {
    val properties = mapOf(
        "version" to project.version,
        "mod_id" to project.property("mod_id"),
        "minecraft_version" to project.property("minecraft_version"),
        "neo_version" to project.property("neo_version"),
        "kotlin_for_forge_version" to project.property("kotlin_for_forge_version"),
    )
    inputs.properties(properties)
    filteringCharset = "UTF-8"

    filesMatching("META-INF/neoforge.mods.toml") {
        expand(properties)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(targetJavaVersion)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.fromTarget(targetJavaVersion.toString()))
}

tasks.jar {
    from("LICENSE.txt") {
        rename { "${it}_${project.base.archivesName.get()}" }
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = project.property("archives_base_name") as String
            from(components["java"])
        }
    }

    repositories {

    }
}
