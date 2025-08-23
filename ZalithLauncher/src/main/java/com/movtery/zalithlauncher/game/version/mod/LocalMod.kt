package com.movtery.zalithlauncher.game.version.mod

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.movtery.zalithlauncher.game.addons.modloader.ModLoader
import com.movtery.zalithlauncher.utils.logging.Logger.lWarning
import kotlinx.io.IOException
import org.apache.commons.io.FileUtils
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** 本地模组信息 */
class LocalMod(
    /** 本地模组对应的文件 */
    modFile: File,

    /** 本地模组对应的文件的大小 */
    val fileSize: Long,

    /** 模组ID */
    val id: String,

    /** 模组所属的加载器 */
    val loader: ModLoader,

    /** 模组的显示名称 */
    val name: String,

    /** 模组描述 */
    val description: String? = null,

    /** 模组版本 */
    val version: String? = null,

    /** 模组的作者列表 */
    val authors: List<String>,

    /** 模组的图标 */
    val icon: ByteArray? = null,

    /**
     * 标记是否为非模组
     */
    val notMod: Boolean = false
) {
    var file by mutableStateOf(modFile)
        private set

    /**
     * 禁用模组
     */
    fun disable() {
        val currentPath = file.absolutePath
        if (file.isDisabled()) return

        val newFile = File("$currentPath.disabled")
        if (!file.renameToSafely(newFile)) return

        file = newFile
    }

    /**
     * 启用模组
     */
    fun enable() {
        val currentPath = file.absolutePath
        if (file.isEnabled()) return

        val newPath = currentPath.substring(0, currentPath.length - ".disabled".length)
        val newFile = File(newPath)
        if (!file.renameToSafely(newFile)) return

        file = newFile
    }

    private fun File.renameToSafely(dest: File): Boolean {
        return try {
            dest.parentFile?.mkdirs()
            Files.move(
                this.toPath(),
                dest.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
            true
        } catch (e: IOException) {
            lWarning("Failed to rename file {$this} to $dest!", e)
            false
        }
    }

    companion object {
        /**
         * 模组是否启用
         */
        fun File.isEnabled(): Boolean = !absolutePath.endsWith(".disabled", ignoreCase = true)

        /**
         * 模组是否禁用
         */
        fun File.isDisabled(): Boolean = !this.isEnabled()

        /**
         * 创建一个非模组文件
         */
        fun createNotMod(file: File): LocalMod = LocalMod(
            modFile = file,
            fileSize = FileUtils.sizeOf(file),
            id = "",
            loader = ModLoader.UNKNOWN,
            name = file.name,
            description = null,
            version = null,
            authors = emptyList(),
            icon = null,
            notMod = true
        )
    }
}