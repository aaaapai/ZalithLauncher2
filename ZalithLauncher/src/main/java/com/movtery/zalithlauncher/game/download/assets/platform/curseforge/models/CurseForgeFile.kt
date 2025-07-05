package com.movtery.zalithlauncher.game.download.assets.platform.curseforge.models

import com.movtery.zalithlauncher.game.download.assets.platform.PlatformDependencyType
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformReleaseType
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformVersion
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray

@Serializable
class CurseForgeFile(
    /**
     * 文件 ID
     */
    @SerialName("id")
    val id: Int,

    /**
     * 与此文件所属的项目的相关的游戏 ID
     */
    @SerialName("gameId")
    val gameId: Int,

    /**
     * 项目 ID
     */
    @SerialName("modId")
    val modId: Int,

    /**
     * 文件是否可供下载
     */
    @SerialName("isAvailable")
    val isAvailable: Boolean,

    /**
     * 文件的展示名称
     */
    @SerialName("displayName")
    val displayName: String,

    /**
     * 确切的文件名
     */
    @SerialName("fileName")
    val fileName: String? = null,

    /**
     * 文件的发布类型
     */
    @SerialName("releaseType")
    val releaseType: PlatformReleaseType,

    /**
     * 文件的状态
     */
    @SerialName("fileStatus")
    val fileStatus: Int,

    /**
     * 文件哈希值（即 md5 或 sha1）
     */
    @SerialName("hashes")
    val hashes: Array<Hash>,

    /**
     * 文件的时间戳
     */
    @SerialName("fileDate")
    val fileDate: String,

    /**
     * 文件长度（以字节为单位）
     */
    @SerialName("fileLength")
    val fileLength: Long,

    /**
     * 文件的下载量
     */
    @SerialName("downloadCount")
    val downloadCount: Long,

    /**
     * 文件在硬盘上的大小
     */
    @SerialName("fileSizeOnDisk")
    val fileSizeOnDisk: Long? = null,

    /**
     * 文件的下载 URL
     */
    @SerialName("downloadUrl")
    val downloadUrl: String? = null,

    /**
     * 此文件相关的游戏版本列表
     */
    @SerialName("gameVersions")
    val gameVersions: Array<String>,

    /**
     * 用于按游戏版本排序的元数据
     */
    @SerialName("sortableGameVersions")
    val sortableGameVersions: JsonArray,

    /**
     * 依赖项文件列表
     */
    @SerialName("dependencies")
    val dependencies: Array<Dependency>,

    @SerialName("exposeAsAlternative")
    val exposeAsAlternative: Boolean? = null,

    @SerialName("parentProjectFileId")
    val parentProjectFileId: Int? = null,

    @SerialName("alternateFileId")
    val alternateFileId: Int? = null,

    @SerialName("isServerPack")
    val isServerPack: Boolean? = null,

    @SerialName("serverPackFileId")
    val serverPackFileId: Int? = null,

    @SerialName("isEarlyAccessContent")
    val isEarlyAccessContent: Boolean? = null,

    @SerialName("earlyAccessEndDate")
    val earlyAccessEndDate: String? = null,

    @SerialName("fileFingerprint")
    val fileFingerprint: Long,

    @SerialName("modules")
    val modules: Array<Module>? = null
) : PlatformVersion {
    @Serializable
    class Hash(
        @SerialName("value")
        val value: String,
        @SerialName("algo")
        val algo: Algo
    ) {
        @Serializable(with = Algo.Serializer::class)
        enum class Algo(val code: Int) {
            SHA1(1),
            MD5(2);

            companion object {
                private val map = entries.associateBy { it.code }
                fun fromCode(code: Int): Algo = map[code] ?: error("Unknown algo code: $code")
            }

            object Serializer : KSerializer<Algo> {
                override val descriptor: SerialDescriptor =
                    PrimitiveSerialDescriptor("Algo", PrimitiveKind.INT)

                override fun deserialize(decoder: Decoder): Algo {
                    val code = decoder.decodeInt()
                    return Algo.fromCode(code)
                }

                override fun serialize(encoder: Encoder, value: Algo) {
                    encoder.encodeInt(value.code)
                }
            }
        }
    }

    @Serializable
    class Dependency(
        @SerialName("modId")
        val modId: Int,
        @SerialName("relationType")
        val relationType: PlatformDependencyType
    )

    @Serializable
    class Module(
        @SerialName("name")
        val name: String,
        @SerialName("fingerprint")
        val fingerprint: Long
    )

    companion object {
        fun CurseForgeFile.fixedFileUrl(): String? {
            return downloadUrl
                ?: if (fileName != null) {
                    "https://edge.forgecdn.net/files/${id / 1000}/${id % 1000}/${fileName}"
                } else {
                    null
                }
        }
    }
}