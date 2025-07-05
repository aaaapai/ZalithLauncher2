package com.movtery.zalithlauncher.game.download.assets.platform.modrinth.models

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Modrinth搜索过滤器
 */
interface ModrinthFacet {
    /**
     * 过滤器名称
     */
    fun facetName(): String

    /**
     * 过滤器值
     */
    fun facetValue(): String

    /**
     * 转换为Modrinth接受的格式
     */
    fun describe(): String? = "${facetName()}:${facetValue()}"
}

/**
 * Minecraft 版本过滤器
 */
class VersionFacet(val version: String) : ModrinthFacet {
    override fun facetValue(): String = version
    override fun facetName(): String = "versions"
}

/**
 * 项目类型
 */
enum class ProjectTypeFacet : ModrinthFacet {
    MOD {
        override fun facetValue(): String = "mod"
    },

    MODPACK {
        override fun facetValue(): String = "modpack"
    },

    RESOURCE_PACK {
        override fun facetValue(): String = "resourcepack"
    },

    SHADER {
        override fun facetValue(): String = "shader"
    };

    override fun facetName(): String = "project_type"
}

/**
 * 转换为Modrinth接受的格式
 */
fun List<ModrinthFacet>.toFacetsString(): String {
    val rawFacets = this.map { listOfNotNull(it.describe()) }
    return Json.encodeToString(rawFacets)
}