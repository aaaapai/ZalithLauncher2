package com.movtery.zalithlauncher.game.version.installed.utils

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.movtery.zalithlauncher.game.addons.modloader.ModLoader
import com.movtery.zalithlauncher.game.version.installed.VersionInfo
import com.movtery.zalithlauncher.utils.logging.Logger.lError
import com.movtery.zalithlauncher.utils.logging.Logger.lWarning
import java.io.File

class VersionInfoUtils {
    companion object {
        private const val VERSION_PATTERN = """(\d+\.\d+\.\d+|\d{2}w\d{2}[a-z])"""

        // "1.20.4-OptiFine_HD_U_I7_pre3"       -> 1.20.4
        // "1.21.3-OptiFine_HD_U_J2_pre6"       -> 1.21.3
        private val OPTIFINE_ID_REGEX = """$VERSION_PATTERN-OptiFine""".toRegex()
        // "1.20.2-forge-48.1.0"                -> 1.20.2
        // "1.21.3-forge-53.0.23"               -> 1.21.3
        private val FORGE_REGEX = """$VERSION_PATTERN-forge""".toRegex()
        // "1.7.10-Forge10.13.4.1614-1.7.10"    -> 1.7.10
        private val FORGE_OLD_REGEX = """^$VERSION_PATTERN-Forge""".toRegex()
        // "fabric-loader-0.15.7-1.20.4"        -> 1.20.4
        // "fabric-loader-0.16.9-1.21.3"        -> 1.21.3
        private val FABRIC_REGEX = """fabric-loader-[\w.-]+-$VERSION_PATTERN""".toRegex()
        // "quilt-loader-0.23.1-1.20.4"         -> 1.20.4
        // "quilt-loader-0.27.1-beta.1-1.21.3"  -> 1.21.3
        private val QUILT_REGEX = """quilt-loader-[\w.-]+-$VERSION_PATTERN""".toRegex()

        private val LOADER_DETECTORS = listOf<(String) -> String?>(
            { id ->
                OPTIFINE_ID_REGEX.find(id)?.groupValues?.get(1)
            },
            { id ->
                FORGE_REGEX.find(id)?.groupValues?.get(1)
            },
            { id ->
                FORGE_OLD_REGEX.find(id)?.groupValues?.get(1)
            },
            { id ->
                FABRIC_REGEX.find(id)?.groupValues?.get(1)
            },
            { id ->
                QUILT_REGEX.find(id)?.groupValues?.get(1)
            }
        )

        /**
         * 在版本的json文件中，找到版本信息
         * @return 版本号、ModLoader信息
         */
        fun parseJson(jsonFile: File): VersionInfo? {
            return runCatching {
                val jsonObject = JsonParser.parseString(jsonFile.readText()).asJsonObject
                val quickPlay = runCatching {
                    ensureQuickPlay(jsonObject)
                }.getOrElse { e ->
                    lWarning("Failed to parse Quick Play", e)
                    VersionInfo.QuickPlay(
                        hasQuickPlaysSupport = false,
                        isQuickPlaySingleplayer = false,
                        isQuickPlayMultiplayer = false
                    )
                }
                val (versionId, loaderInfo) = detectMinecraftAndLoader(jsonObject)
                VersionInfo(versionId, quickPlay, loaderInfo)
            }.getOrElse {
                lError("Error parsing version json", it)
                null
            }
        }

        /**
         * 确认是否可以使用 Quick Play
         */
        private fun ensureQuickPlay(versionJson: JsonObject): VersionInfo.QuickPlay {
            var hasQuickPlaysSupport = false
            var isQuickPlaySingleplayer = false
            var isQuickPlayMultiplayer = false

            versionJson.getAsJsonObject("arguments")?.getAsJsonArray("game")?.forEach outerFor@{ element ->
                if (!element.isJsonObject) return@outerFor

                element.asJsonObject.getAsJsonArray("rules")?.forEach innerFor@{ rule ->
                    if (!rule.isJsonObject) return@innerFor

                    val features = rule.asJsonObject.getAsJsonObject("features") ?: return@innerFor

                    hasQuickPlaysSupport = hasQuickPlaysSupport || features.get("has_quick_plays_support")?.asBoolean ?: false
                    isQuickPlaySingleplayer = isQuickPlaySingleplayer || features.get("is_quick_play_singleplayer")?.asBoolean ?: false
                    isQuickPlayMultiplayer = isQuickPlayMultiplayer || features.get("is_quick_play_multiplayer")?.asBoolean ?: false

                    if (hasQuickPlaysSupport && isQuickPlaySingleplayer && isQuickPlayMultiplayer) {
                        return@ensureQuickPlay VersionInfo.QuickPlay(
                            hasQuickPlaysSupport = true,
                            isQuickPlaySingleplayer = true,
                            isQuickPlayMultiplayer = true
                        )
                    }
                }
            }

            return VersionInfo.QuickPlay(
                hasQuickPlaysSupport = hasQuickPlaysSupport,
                isQuickPlaySingleplayer = isQuickPlaySingleplayer,
                isQuickPlayMultiplayer = isQuickPlayMultiplayer
            )
        }

        private fun detectMinecraftAndLoader(versionJson: JsonObject): Pair<String, VersionInfo.LoaderInfo?> {
            val mcVersion = extractMinecraftVersion(versionJson)
            val loaderInfo = detectModLoader(versionJson)
            return mcVersion to loaderInfo
        }

        private fun extractMinecraftVersion(json: JsonObject): String {
            //尝试识别HMCL版本
            if (json.has("patches") && json.get("patches").isJsonArray) {
                val patches = json.getAsJsonArray("patches")
                if (patches.size() > 0) {
                    val minecraft = patches[0].asJsonObject
                    if (minecraft.has("version")) {
                        return minecraft.get("version").asString
                    }
                }
            }

            //尝试从 LaunchFor (ZL安装的版本) 获取信息
            json.getAsJsonObject("launchFor")
                ?.getAsJsonArray("infos")
                ?.firstOrNull { it.asJsonObject["name"]?.asString == "Minecraft" }
                ?.asJsonObject
                ?.getAsJsonPrimitive("version")
                ?.asString
                ?.let {
                    return it
                }

            //从minecraft库中获取
            json.getAsJsonArray("libraries")?.forEach { lib ->
                val (group, artifact, version) = lib.asJsonObject["name"].asString.split(":").let {
                    Triple(it[0], it[1], it.getOrNull(2) ?: "")
                }
                if (group == "net.minecraft" && (artifact == "client" || artifact == "server")) {
                    return version
                }
            }

            val id = json["id"].asString
            return if (json.has("inheritsFrom")) json["inheritsFrom"].asString
            //尝试从ID中解析MC版本
            else LOADER_DETECTORS.firstNotNullOfOrNull { it(id) } ?: id
        }

        /**
         * 通过库判断ModLoader信息：ModLoader名称、版本
         * @param versionJson 版本json对象
         */
        private fun detectModLoader(versionJson: JsonObject): VersionInfo.LoaderInfo? {
            versionJson.getAsJsonArray("libraries")?.forEach { libElement ->
                val lib = libElement.asJsonObject
                val (group, artifact, version) = lib.get("name").asString.split(":").let {
                    Triple(it[0], it[1], it.getOrNull(2) ?: "")
                }

                when {
                    //Fabric
                    group == "net.fabricmc" && artifact == "fabric-loader" ->
                        return VersionInfo.LoaderInfo(ModLoader.FABRIC, version)

                    //Forge
                    group == "net.minecraftforge" && (artifact == "forge" || artifact == "fmlloader") -> {
                        val forgeVersion = when {
                            //新版：1.21.4-54.0.26                 -> 54.0.26
                            version.count { it == '-' } == 1 -> version.substringAfterLast('-')
                            //旧版：1.7.10-10.13.4.1614-1.7.10     -> 10.13.4.1614
                            version.count { it == '-' } >= 2 -> version.split("-").let { parts ->
                                when {
                                    parts.size >= 3 && parts[0] == parts.last() -> parts[1]
                                    else -> version
                                }
                            }
                            else -> version
                        }
                        return VersionInfo.LoaderInfo(ModLoader.FORGE, forgeVersion)
                    }

                    //NeoForge
                    group == "net.neoforged.fancymodloader" && artifact == "loader" -> {
                        val neoVersion = versionJson.getAsJsonObject("arguments")
                            ?.getAsJsonArray("game")
                            ?.findNeoForgeVersion()
                            ?: version
                        return VersionInfo.LoaderInfo(ModLoader.NEOFORGE, neoVersion)
                    }

                    //OptiFine
                    (group == "optifine" || group == "net.optifine") && artifact == "OptiFine" ->
                        return VersionInfo.LoaderInfo(ModLoader.OPTIFINE, version)

                    //Quilt
                    group == "org.quiltmc" && artifact == "quilt-loader" ->
                        return VersionInfo.LoaderInfo(ModLoader.QUILT, version)

                    //LiteLoader
                    group == "com.mumfrey" && artifact == "liteloader" ->
                        return VersionInfo.LoaderInfo(ModLoader.LITE_LOADER, version)
                }
            }

            return null
        }

        /**
         * NeoForge会将版本号存放到游戏参数内
         * 尝试在 arguments: { "game": [] } 中寻找NeoForge的版本
         */
        private fun JsonArray.findNeoForgeVersion(): String? {
            val args = this.mapNotNull { it.takeIf(JsonElement::isJsonPrimitive)?.asString }
            return args.zipWithNext().find { it.first == "--fml.neoForgeVersion" }?.second
        }
    }
}