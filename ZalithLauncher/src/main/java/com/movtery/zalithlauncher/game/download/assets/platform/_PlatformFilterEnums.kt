package com.movtery.zalithlauncher.game.download.assets.platform

import com.movtery.zalithlauncher.game.download.assets.platform.curseforge.models.CurseForgeClassID
import com.movtery.zalithlauncher.game.download.assets.platform.modrinth.models.ProjectTypeFacet

interface PlatformFilterCode {
    fun getDisplayName(): Int
    fun index(): Int
}

interface PlatformDisplayLabel {
    fun getDisplayName(): String
    fun index(): Int
}

enum class PlatformClasses(
    val curseforge: CurseForgeClassID,
    val modrinth: ProjectTypeFacet?
) {
    MOD(
        curseforge = CurseForgeClassID.MOD,
        modrinth = ProjectTypeFacet.MOD
    ),
    MOD_PACK(
        curseforge = CurseForgeClassID.MOD_PACK,
        modrinth = ProjectTypeFacet.MODPACK
    ),
    RESOURCE_PACK(
        curseforge = CurseForgeClassID.RESOURCE_PACK,
        modrinth = ProjectTypeFacet.RESOURCE_PACK
    ),
    SAVES(
        curseforge = CurseForgeClassID.SAVES,
        modrinth = null
    ),
    SHADERS(
        curseforge = CurseForgeClassID.SHADERS,
        modrinth = ProjectTypeFacet.SHADER
    )
}