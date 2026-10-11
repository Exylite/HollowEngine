import org.gradle.jvm.tasks.Jar

plugins {
    java
    id("architectury-plugin")
    id("dev.architectury.loom-no-remap")
}

val modId: String by properties
val modVersion: String by properties
val modGroup: String by properties
val minecraftVersion: String by rootProject.properties
val enabledPlatforms = (rootProject.property("enabledPlatforms") as String).split(',').map(String::trim).toTypedArray()
val fabricLoaderVersion: String by rootProject.properties

group = modGroup
version = modVersion
base.archivesName.set("HollowEngineBridge")

val sourceSets = extensions.getByType<SourceSetContainer>()

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/")
    maven("https://maven.architectury.dev/")
    maven("https://repo.spongepowered.org/repository/maven-public/")
    mavenLocal()
    flatDir { dirs(rootProject.file("libs")) }
    maven("https://api.modrinth.com/maven") {
        content { includeGroup("maven.modrinth") }
    }
}

architectury {
    common(*enabledPlatforms)
}

loom {
    silentMojangMappingsLicense()

    val accessWidener = rootProject.file("runtime/src/main/resources/$modId.accesswidener")
    if (accessWidener.exists()) {
        accessWidenerPath.set(accessWidener)
    }
}

dependencies {
    "minecraft"("com.mojang:minecraft:$minecraftVersion")

    implementation("maven.modrinth:iris:1.11.4+26.2-fabric")
    implementation("net.fabricmc:fabric-loader:$fabricLoaderVersion")

    compileOnly("org.spongepowered:mixin:0.8.7")
    compileOnly("io.github.llamalad7:mixinextras-common:0.5.5")
    compileOnly("org.jetbrains:annotations:24.1.0")
}

sourceSets.named("main").configure {
    java.setSrcDirs(listOf(rootProject.file("bridge/src/main/java")))
    resources.setSrcDirs(listOf(rootProject.file("bridge/src/main/resources")))
}

tasks.named<ProcessResources>("processResources") {
    filesMatching("hollowengine.bridge.mixins.json") {
        expand("refmap" to "$modId.bridge.refmap.json")
    }
}

tasks.named<Jar>("jar") {
    archiveClassifier.set("")
}

// Without remapping Loom has no named jar of its own, but dependents still ask for the Mojang-named
// classes by this configuration.
configurations.create("namedElements") {
    isCanBeConsumed = true
    isCanBeResolved = false
    configurations.findByName("api")?.let { extendsFrom(it) }
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_API))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
    }
    outgoing.artifact(tasks.named<Jar>("jar"))
}
