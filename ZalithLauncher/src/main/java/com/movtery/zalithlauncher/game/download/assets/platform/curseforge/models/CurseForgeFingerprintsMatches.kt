package com.movtery.zalithlauncher.game.download.assets.platform.curseforge.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class CurseForgeFingerprintsMatches(
    val data: Result
) {
    @Serializable
    data class Result(
        val isCacheBuilt: Boolean,
        val exactMatches: List<FingerprintMatch>? = null,
        val exactFingerprints: List<Long>? = null,
        val partialMatches: List<FingerprintMatch>? = null,
        val partialMatchFingerprints: JsonElement? = null,
        val additionalProperties: List<Long>? = null,
        val installedFingerprints: List<Long>? = null,
        val unmatchedFingerprints: List<Long>? = null
    ) {
        @Serializable
        data class FingerprintMatch(
            val id: Int,
            val file: CurseForgeFile,
            val latestFiles: List<CurseForgeFile>
        )
    }
}