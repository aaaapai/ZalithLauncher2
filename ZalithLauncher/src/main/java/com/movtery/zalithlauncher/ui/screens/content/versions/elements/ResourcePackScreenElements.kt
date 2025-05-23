package com.movtery.zalithlauncher.ui.screens.content.versions.elements

import android.util.Log
import com.movtery.zalithlauncher.utils.json.parseToJson
import com.movtery.zalithlauncher.utils.string.StringUtils.Companion.stripColorCodes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import java.io.File
import java.util.zip.ZipFile

/** 资源包加载状态 */
sealed interface ResourcePackState {
    data object None : ResourcePackState
    /** 正在加载所有资源包 */
    data object Loading : ResourcePackState
}

/** 资源包操作状态 */
sealed interface ResourcePackOperation {
    data object None : ResourcePackOperation
    /** 执行任务中 */
    data object Progress : ResourcePackOperation
    /** 重命名资源包输入对话框 */
    data class RenamePack(val packInfo: ResourcePackInfo) : ResourcePackOperation
    /** 删除资源包输入对话框 */
    data class DeletePack(val packInfo: ResourcePackInfo) : ResourcePackOperation
}

/**
 * 简易的资源包过滤器
 */
data class ResourcePackFilter(
    val onlyShowValid: Boolean,
    val filterName: String
)

/**
 * 简易过滤器，过滤指定名称的资源包
 */
fun List<ResourcePackInfo>.filterPacks(filter: ResourcePackFilter) = this.filter {
    val valid = !filter.onlyShowValid || it.isValid
    val nameMatched = filter.filterName.isEmpty() ||
            //用清除了格式化代码的名称进行判断
            it.rawName.contains(filter.filterName, true)
    valid && nameMatched
}

/**
 * 资源包信息类
 */
data class ResourcePackInfo(
    /** 资源包文件 */
    val file: File,
    /** 提前计算好的文件大小 */
    val fileSize: Long,
    /** 清除颜色替换符后的文件名 */
    val rawName: String = file.name.stripColorCodes(),
    /** 显示名称（如果是压缩包类型的资源包，将被去掉扩展名） */
    val displayName: String = if (file.isDirectory) file.name else file.nameWithoutExtension,
    /** 资源包是否有效 */
    val isValid: Boolean,
    /** 资源包的描述信息 */
    val description: String?,
    /** 资源包的格式版本 */
    val packFormat: Int?,
    /** 资源包的图标 */
    val icon: ByteArray?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ResourcePackInfo

        if (isValid != other.isValid) return false
        if (packFormat != other.packFormat) return false
        if (file != other.file) return false
        if (description != other.description) return false
        if (icon != null) {
            if (other.icon == null) return false
            if (!icon.contentEquals(other.icon)) return false
        } else if (other.icon != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isValid.hashCode()
        result = 31 * result + (packFormat ?: 0)
        result = 31 * result + file.hashCode()
        result = 31 * result + (description?.hashCode() ?: 0)
        result = 31 * result + (icon?.contentHashCode() ?: 0)
        return result
    }
}

/**
 * 解析资源包文件，游戏内仅支持加载文件夹、文件后缀为zip的资源包
 * @param file 资源包文件
 */
suspend fun parseResourcePack(file: File): ResourcePackInfo? = withContext(Dispatchers.IO) {
    runCatching {
        var isValid = false
        val metaContent: String
        val iconBytes: ByteArray?

        if (file.isDirectory) { //文件夹形式的资源包
            //资源包元数据
            val metaFile = File(file, "pack.mcmeta")
            if (!metaFile.exists()) return@withContext null
            metaContent = metaFile.readText()
            //尝试读取资源包的图标
            val iconFile = File(file, "pack.png")
            iconBytes = if (iconFile.exists()) iconFile.readBytes() else null
        } else if (file.extension == "zip") { //压缩包形式的资源包
            ZipFile(file).use { zip ->
                //资源包元数据
                val metaEntry = zip.getEntry("pack.mcmeta") ?: return@withContext null
                metaContent = zip.getInputStream(metaEntry).bufferedReader().readText()
                //尝试读取资源包的图标
                val iconEntry = zip.getEntry("pack.png")
                iconBytes = iconEntry?.let { zip.getInputStream(it).readBytes() }
            }
        } else {
            //其他格式的资源包，按照游戏的做法，直接忽略
            return@withContext null
        }

        val metaJson = metaContent.parseToJson()
        val pack = metaJson.get("pack")?.also {
            //这个pack必须存在，否则将不是有效的格式
            isValid = true
        }?.asJsonObject

        ResourcePackInfo(
            file = file,
            fileSize = FileUtils.sizeOf(file),
            isValid = isValid,
            description = pack?.get("description")?.asString,
            packFormat = pack?.get("pack_format")?.asInt,
            icon = iconBytes
        )
    }.onFailure {
        Log.w("ParseResourcePack", "Failed to parse the resource package: ${file.absolutePath}", it)
    }.getOrNull()
}