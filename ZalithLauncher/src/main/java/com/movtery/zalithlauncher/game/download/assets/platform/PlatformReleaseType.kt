package com.movtery.zalithlauncher.game.download.assets.platform

import androidx.compose.ui.graphics.Color
import com.movtery.zalithlauncher.R
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = PlatformReleaseType.Serializer::class)
enum class PlatformReleaseType(val curseforgeCode: Int, val textRes: Int, val color: Color) {
    @SerialName("release")
    RELEASE(1, R.string.download_assets_release_type_release, Color(0xFF00AE5C)),

    @SerialName("beta")
    BETA(2, R.string.download_assets_release_type_beta, Color(0xFFDF8225)),

    @SerialName("alpha")
    ALPHA(3, R.string.download_assets_release_type_alpha, Color(0xFFCA2245));

    companion object {
        private val map = PlatformReleaseType.entries.associateBy { it.curseforgeCode }
        fun fromCurseForgeCode(code: Int): PlatformReleaseType = map[code] ?: error("Unknown release code: $code")
    }

    object Serializer : KSerializer<PlatformReleaseType> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("PlatformReleaseType", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): PlatformReleaseType {
            return when (val input = runCatching { decoder.decodeInt() }.getOrNull()) {
                null -> {
                    val name = decoder.decodeString().lowercase()
                    when (name) {
                        "release" -> RELEASE
                        "beta" -> BETA
                        "alpha" -> ALPHA
                        else -> error("Unknown type name: $name")
                    }
                }
                else -> {
                    PlatformReleaseType.fromCurseForgeCode(input)
                }
            }
        }

        override fun serialize(encoder: Encoder, value: PlatformReleaseType) {
            val name = when (value) {
                RELEASE -> "release"
                BETA -> "beta"
                ALPHA -> "alpha"
            }
            encoder.encodeString(name)
        }
    }
}