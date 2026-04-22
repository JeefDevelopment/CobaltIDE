import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	kotlin("jvm") version "2.3.10"
	id("fabric-loom") version "1.13.6"
}

version = providers.gradleProperty("mod_version").get()
group = providers.gradleProperty("maven_group").get()

repositories {
	maven("https://maven.nucleoid.xyz/") { name = "Nucleoid" }
	maven("https://maven.impactdev.net/repository/development/")
	maven("https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/") // Often required for dependencies
}

dependencies {
	// To change the versions see the gradle.properties file
	minecraft("com.mojang:minecraft:${providers.gradleProperty("minecraft_version").get()}")

	mappings("net.fabricmc:yarn:${providers.gradleProperty("yarn_mappings").get()}:v2")
	modImplementation("net.fabricmc:fabric-loader:${providers.gradleProperty("loader_version").get()}")

	// Fabric API. This is technically optional, but you probably want it anyway.
	modImplementation("net.fabricmc.fabric-api:fabric-api:${providers.gradleProperty("fabric_api_version").get()}")
	modImplementation("net.fabricmc:fabric-language-kotlin:${providers.gradleProperty("fabric_kotlin_version").get()}")
	modImplementation("com.cobblemon:fabric:1.7.3+1.21.1")
	modImplementation("eu.pb4:polymer-core:0.9.19+1.21.1")
	modImplementation("eu.pb4:sgui:1.6.1+1.21.1")
	modImplementation("eu.pb4:polymer-virtual-entity:0.9.19+1.21.1")
	modImplementation("eu.pb4:polymer-resource-pack:0.9.19+1.21.1")
	modImplementation("eu.pb4:factorytools:0.3.2+1.21")
	implementation("net.impactdev.impactor.api:economy:5.3.5")
	implementation(fileTree("dir" to "libs", "include" to "*.jar"))
	implementation("eu.pb4:placeholder-api:2.4.2+1.21")
	implementation("ca.landonjw.gooeylibs:api:3.1.0-1.21.1-SNAPSHOT")
}
tasks.processResources {
	inputs.property("version", version)

	filesMatching("fabric.mod.json") {
		expand("version" to version)
	}
}
kotlin {
	compilerOptions {
		jvmTarget = JvmTarget.JVM_21
		freeCompilerArgs.add("-Xallow-any-scripts-in-source-roots")
	}
}

java {
	// Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
	// if it is present.
	// If you remove this line, sources will not be generated.
	withSourcesJar()

	sourceCompatibility = JavaVersion.VERSION_21
	targetCompatibility = JavaVersion.VERSION_21
}
