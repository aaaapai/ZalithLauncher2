plugins {
    id("java-library")
}

group = "org.lwjgl.glfw"

configurations.getByName("default").isCanBeResolved = true

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveBaseName.set("lwjgl-glfw-classes")
    destinationDirectory.set(file("../ZalithLauncher/src/main/assets/components/lwjgl3/"))
    // Auto update the version with a timestamp so the project jar gets updated by Pojav
    doLast {
        val versionFile = file("../ZalithLauncher/src/main/assets/components/lwjgl3/version")
        versionFile.writeText(System.currentTimeMillis().toString())
    }
    from({
        configurations.getByName("default").map {
            println(it.name)
            if (it.isDirectory) it else zipTree(it)
        }
    })
    exclude("net/java/openjdk/cacio/ctc/**")
    manifest {
        attributes("Manifest-Version" to "3.4.0")
        attributes("Automatic-Module-Name" to "org.lwjgl")
        attributes("Specification-Title" to "Lightweight Java Game Library - Core")
        attributes("Specification-Version" to "3.4.0")
        attributes("Specification-Vendor" to "lwjgl.org")
        attributes("Implementation-Title" to "lwjgl")
        attributes("Implementation-Version" to "SNAPSHOT")
        attributes("Implementation-Vendor" to "lwjgl.org")
        attributes("Multi-Release" to "true")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    compileOnly(fileTree("dir" to "compileOnly", "include" to listOf("*.jar")))
}
