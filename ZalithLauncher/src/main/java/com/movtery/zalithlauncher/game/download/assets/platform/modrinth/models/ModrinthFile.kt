package com.movtery.zalithlauncher.game.download.assets.platform.modrinth.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class ModrinthFile(
    /** 文件哈希的映射。键为哈希算法，值为哈希的字符串形式 */
    @SerialName("hashes")
    val hashes: Hash,

    /** 文件的直接链接 */
    @SerialName("url")
    val url: String,

    /** 文件名称 */
    @SerialName("filename")
    val fileName: String,

    /**
     * 该文件是否为其版本的主文件。每个版本最多只有一个文件会被设置为主文件。如果没有任何主文件，则可以推断第一个文件为主文件。
     */
    @SerialName("primary")
    val primary: Boolean,

    /** 文件的大小，单位为字节 */
    @SerialName("size")
    val size: Long,

    /** 附加文件的类型，主要用于将资源包添加到数据包中 */
    @SerialName("file_type")
    val fileType: String? = null
) {
    @Serializable
    class Hash(
        @SerialName("sha1")
        val sha1: String,

        @SerialName("sha512")
        val sha512: String
    )

    companion object {
        fun Array<ModrinthFile>.getPrimary(): ModrinthFile? {
            val files = this.takeIf { it.isNotEmpty() } ?: run {
                return null
            }
            return files.find { it.primary } ?: this[0] //仅下载主文件
        }
    }
}