package com.movtery.zalithlauncher.game.download.assets.platform.modrinth.models

import com.movtery.zalithlauncher.game.download.assets.platform.PlatformDependencyType
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformReleaseType
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformVersion
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class ModrinthVersion(
    /** 版本的显示名称 */
    @SerialName("name")
    val name: String,

    /** 版本号 */
    @SerialName("version_number")
    val versionNumber: String,

    /** 版本的变更日志 */
    @SerialName("changelog")
    val changelog: String? = null,

    /** 此版本所依赖的特定项目版本的列表 */
    @SerialName("dependencies")
    val dependencies: Array<Dependency>,

    /** 支持的游戏版本 */
    @SerialName("game_versions")
    val gameVersions: Array<String>,

    /** 该版本的发布渠道 */
    @SerialName("version_type")
    val versionType: PlatformReleaseType,

    /** 该版本支持的模组加载器。对于资源包，使用“minecraft” */
    @SerialName("loaders")
    val loaders: Array<String>,

    /** 该版本是否为推荐版本 */
    @SerialName("featured")
    val featured: Boolean,

    @SerialName("status")
    val status: String,

    @SerialName("requested_status")
    val requestedStatus: String? = null,

    /** 版本的ID，以 base62 字符串编码 */
    @SerialName("id")
    val id: String,

    /** 该版本所属项目的ID */
    @SerialName("project_id")
    val projectId: String,

    /** 发布该版本的作者ID */
    @SerialName("author_id")
    val authorId: String,

    @SerialName("date_published")
    val datePublished: String,

    /** 该版本的下载次数 */
    @SerialName("downloads")
    val downloads: Long,

    /** 该版本更新日志的链接。始终为 null，仅为兼容旧版本而保留 */
    @SerialName("changelog_url")
    val changelogUrl: String? = null,

    /** 该版本可下载文件的列表 */
    @SerialName("files")
    val files: Array<ModrinthFile>
) : PlatformVersion {
    @Serializable
    class Dependency(
        /** 这个版本所依赖的版本的ID */
        @SerialName("version_id")
        val versionId: String? = null,

        /** 这个版本所依赖的项目的ID */
        @SerialName("project_id")
        val projectId: String? = null,

        /** 依赖项的文件名，主要用于在模组包中显示外部依赖项 */
        @SerialName("file_name")
        val fileName: String? = null,

        /** 该版本的依赖类型 */
        @SerialName("dependency_type")
        val dependencyType: PlatformDependencyType
    )
}