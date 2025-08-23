package com.movtery.zalithlauncher.game.renderer

import com.movtery.zalithlauncher.game.plugin.renderer.RendererPlugin

/**
 * 启动器渲染器实现
 */
interface RendererInterface {
    /**
     * 获取渲染器的ID
     */
    fun getRendererId(): String

    /**
     * 获取渲染器的唯一标识ID
     */
    fun getUniqueIdentifier(): String

    /**
     * 获取渲染器的名称
     */
    fun getRendererName(): String

    /**
     * 获取渲染器的描述
     */
    fun getRendererSummary(): String? = null

    /**
     * 获取渲染器最低兼容版本
     */
    fun getMinMCVersion(): String? = null

    /**
     * 获取渲染器最高兼容版本
     */
    fun getMaxMCVersion(): String? = null

    /**
     * 获取渲染器的环境变量
     */
    fun getRendererEnv(): Lazy<Map<String, String>>

    /**
     * 获取需要dlopen的库
     */
    fun getDlopenLibrary(): Lazy<List<String>>

    /**
     * 获取渲染器的库
     */
    fun getRendererLibrary(): String

    /**
     * 获取EGL名称
     */
    fun getRendererEGL(): String? = null

    companion object {
        fun RendererPlugin.toInterface() = object : RendererInterface {
            override fun getRendererId(): String = id
            override fun getUniqueIdentifier(): String = uniqueIdentifier
            override fun getRendererName(): String = displayName
            override fun getRendererSummary(): String? = summary
            override fun getMinMCVersion(): String? = minMCVer
            override fun getMaxMCVersion(): String? = maxMCVer
            override fun getRendererEnv(): Lazy<Map<String, String>> = lazy { env }
            override fun getDlopenLibrary(): Lazy<List<String>> = lazy { dlopen }
            override fun getRendererLibrary(): String = glName
            override fun getRendererEGL(): String = eglName
        }
    }
}