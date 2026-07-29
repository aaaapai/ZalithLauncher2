plugins {
    id("java-library")
}

group = "org.lwjgl.glfw"
val lwjglVersion = "3.4.3"

val lwjglModules by configurations.creating {
    isCanBeResolved = true
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    lwjglModules(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    compileOnly(fileTree(mapOf("dir" to "compileOnly", "include" to listOf("*.jar"))))
    implementation("org.jspecify:jspecify:1.0.0")
}

tasks.jar {
    doFirst {
        val destDir = destinationDirectory.get().asFile
        destDir.listFiles()?.forEach { file ->
            if (file.isFile && (file.extension == "jar" || file.name == "version")) {
                file.delete()
            }
        }
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveBaseName.set("lwjgl-glfw-classes")
    destinationDirectory.set(file("../ZalithLauncher/src/main/assets/components/lwjgl3"))

    exclude("net/java/openjdk/cacio/ctc/**")

    val excludedModules = listOf(
        "lwjgl-egl.jar",
        "lwjgl-lwjglx.jar",
        "lwjgl-freetype.jar",
        "lwjgl-jemalloc.jar",
        "lwjgl-nanovg.jar",
        "lwjgl-openal.jar",
        "lwjgl-opengl.jar",
        "lwjgl-opengles.jar",
        "lwjgl-mimalloc.jar",
        "lwjgl-stb.jar",
        "lwjgl-tinyfd.jar",
        "lwjgl-shaderc.jar",
        "lwjgl-spvc.jar",
        "lwjgl-vma.jar",
        "lwjgl-vulkan.jar",
        "lwjgl-sdl.jar",
        "lwjgl-spng.jar"
    )

    from({
        val includedModules = lwjglModules.filter { dep ->
            !excludedModules.contains(dep.name)
        }
        val coreJar = includedModules.find { it.name == "lwjgl.jar" }
        val jarList = if (coreJar != null) {
            listOf(coreJar) + includedModules.filter { it != coreJar }
        } else {
            includedModules
        }
        println("Merging LWJGL $lwjglVersion modules in order:")
        jarList.forEach { println("  ${it.name}") }
        jarList.map { if (it.isDirectory) it else zipTree(it) }
    })

    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true

    doLast {
        val versionFile = file("${destinationDirectory.get().asFile}/version")
        versionFile.writeText(System.currentTimeMillis().toString())
    }

    doLast {
        val destDir = destinationDirectory.get().asFile
        excludedModules.forEach { fileName ->
            val depFile = lwjglModules.find { it.name == fileName }
            if (depFile != null) {
                copy {
                    from(depFile)
                    into(destDir)
                }
            } else {
                println("Warning: excluded module '$fileName' not found")
            }
        }
    }

    manifest {
        attributes("Manifest-Version" to lwjglVersion)
        attributes("Automatic-Module-Name" to "org.lwjgl")
        attributes("Specification-Title" to "Lightweight Java Game Library - Core")
        attributes("Implementation-Title" to "lwjgl")
        attributes("Implementation-Version" to "RELEASE")
        attributes("Implementation-Vendor" to "lwjgl.org")
        attributes("Multi-Release" to "true")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
