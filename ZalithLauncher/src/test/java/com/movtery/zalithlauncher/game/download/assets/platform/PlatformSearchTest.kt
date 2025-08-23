package com.movtery.zalithlauncher.game.download.assets.platform

import com.movtery.zalithlauncher.game.download.assets.platform.curseforge.CurseForgeSearchRequest
import com.movtery.zalithlauncher.game.download.assets.platform.curseforge.models.CurseForgeModCategory
import com.movtery.zalithlauncher.game.download.assets.platform.curseforge.models.CurseForgeModLoader
import com.movtery.zalithlauncher.game.download.assets.platform.modrinth.ModrinthSearchRequest
import com.movtery.zalithlauncher.game.download.assets.platform.modrinth.models.ModrinthModCategory
import com.movtery.zalithlauncher.game.download.assets.platform.modrinth.models.ModrinthModLoaderCategory
import com.movtery.zalithlauncher.game.download.assets.platform.modrinth.models.ProjectTypeFacet
import com.movtery.zalithlauncher.game.download.assets.platform.modrinth.models.VersionFacet
import com.movtery.zalithlauncher.utils.file.calculateFileSha1
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.File
import java.nio.file.Paths

class PlatformSearchJsonTest {

    @Test
    fun testSearchWithCurseforge() = runBlocking {
        val request = CurseForgeSearchRequest(
            sortField = PlatformSortField.POPULARITY,
            categories = setOf(
                CurseForgeModCategory.FOOD,
                CurseForgeModCategory.MAGIC
            ),
            gameVersion = "1.21.4",
            modLoader = CurseForgeModLoader.FABRIC,
            index = 0,
            pageSize = 50
        )

        val result = PlatformSearch.searchWithCurseforge(request)
        assertNotNull(result)
        result.let { r ->
            r.data.forEach { project ->
                println("id = ${project.id}")
                println("classId = ${project.classId}")
                println("name = ${project.name}")
                println("categories = ${project.categories.joinToString(",") { it.id.toString() }}")
                println("-----------")
            }
            println("index = ${r.pagination.index}")
            println("resultCount = ${r.pagination.resultCount}")
            println("totalCount = ${r.pagination.totalCount}")
        }
    }

    @Test
    fun testGetProjectFromCurseForge() = runBlocking(Dispatchers.IO) {
        val result = PlatformSearch.getProjectFromCurseForge("419699")
        assertNotNull(result)
        result.let {
            val data = it.data
            println("id = ${data.id}")
            println("classId = ${data.classId}")
            println("name = ${data.name}")
            println("categories = ${data.categories.joinToString(",") { it.id.toString() }}")
        }
    }

    @Test
    fun testGetAllVersionsFromCurseForge() = runBlocking(Dispatchers.IO) {
        val result = PlatformSearch.getAllVersionsFromCurseForge(projectID = "238222")
        result.forEach { file ->
            println("id = ${file.id}")
            println("fileName = ${file.fileName}")
            println("displayName = ${file.displayName}")
            println("gameVersions = ${file.gameVersions.joinToString(",")}")
            println("dependencies = ${file.dependencies.joinToString(",") { it.modId.toString() }}")
            println("downloadCount = ${file.downloadCount}")
            println("downloadUrl = ${file.downloadUrl}")
            println("-----------")
        }
    }

    @Test
    fun testGetVersionByLocalFileFromCurseForge() = runBlocking(Dispatchers.IO) {
        val files = listOf(
            "F:\\Download\\geckolib-forge-1.21.8-5.2.2.jar"
        )
        files.forEach { file ->
            val result = PlatformSearch.getVersionByLocalFileFromCurseForge(
                file = File(file)
            )
            println(result.data.toString())
        }
    }

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Test
    fun testSearchWithModrinth() = runBlocking(Dispatchers.IO) {
        val request = ModrinthSearchRequest(
            facets = listOf(
                ProjectTypeFacet.MOD,
                VersionFacet("1.20.1"),
                VersionFacet("1.21.4"),
                ModrinthModLoaderCategory.FABRIC,
                ModrinthModLoaderCategory.NEOFORGE,
                ModrinthModCategory.MAGIC
            ),
            offset = 0,
            limit = 20
        )
        val result = PlatformSearch.searchWithModrinth(request)
        assertNotNull(result)
        result.let { r ->
            r.hits.forEach { project ->
                println("projectID = ${project.projectId}")
                println("projectType = ${project.projectType}")
                println("title = ${project.title}")
                println("displayCategories = ${project.displayCategories?.joinToString(",")}")
                println("-----------")
            }
            println("offset = ${r.offset}")
            println("limit = ${r.limit}")
            println("totalHits = ${r.totalHits}")
        }
    }

    @Test
    fun testGetProjectFromModrinth() = runBlocking(Dispatchers.IO) {
        val result = PlatformSearch.getProjectFromModrinth("TMVdoKxw")
        println("id = ${result.id}")
        println("projectType = ${result.projectType}")
        println("title = ${result.title}")
        println("categories = ${result.categories.joinToString(",")}")
    }

    @Test
    fun testGetVersionsFromProject() = runBlocking(Dispatchers.IO) {
        val result = PlatformSearch.getVersionsFromModrinth("AANobbMI")
        result.forEach { version ->
            println("id = ${version.id}")
            println("name = ${version.name}")
            println("versionNumber = ${version.versionNumber}")
            println("gameVersions = ${version.gameVersions.joinToString(",")}")
            println("dependencies = ${version.dependencies.joinToString(",") { it.projectId ?: "" }}")
            println("downloads = ${version.downloads}")
            println("files = ${version.files.joinToString(",") { it.fileName } }")
            println("-----------")
        }
    }

    @Test
    fun testGetVersionByLocalFileFromModrinth() = runBlocking(Dispatchers.IO) {
        val files = listOf(
            "F:\\Download\\geckolib-forge-1.21.8-5.2.2.jar"
        )
        files.forEach { file ->
            val result = PlatformSearch.getVersionByLocalFileFromModrinth(
                sha1 = calculateFileSha1(File(file))
            )
            println(result.toString())
        }
    }

    @Suppress("unused")
    private fun writeTestResult(fileName: String, text: String) {
        val testOutputDir = Paths.get("src", "test", "result", "com.movtery.zalithlauncher.game.download.assets.platform")
        val outputFile = testOutputDir.resolve(fileName).toFile()
        outputFile.parentFile?.mkdirs()
        outputFile.writeText(text, Charsets.UTF_8)
    }
}
