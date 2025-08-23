package com.movtery.zalithlauncher.ui.screens.content.versions.elements

import java.io.File

sealed interface ShaderOperation {
    data object None : ShaderOperation
    /** 执行任务中 */
    data object Progress : ShaderOperation
    /** 重命名光影包输入对话框 */
    data class Rename(val info: ShaderPackInfo) : ShaderOperation
    /** 删除光影包对话框 */
    data class Delete(val info: ShaderPackInfo) : ShaderOperation
}

/**
 * 光影包信息
 */
data class ShaderPackInfo(
    val file: File,
    val fileSize: Long
)

/**
 * 简易过滤器，过滤特定的光影包
 */
fun List<ShaderPackInfo>.filterShaders(
    nameFilter: String
) = this.filter {
    nameFilter.isEmpty() || it.file.name.contains(nameFilter, true)
}