package com.movtery.zalithlauncher.game.plugin.renderer

import android.content.Context
import android.content.pm.ApplicationInfo
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.renderer.Renderers

/**
 * FCL、ZalithLauncher 渲染器插件，同时支持使用本地渲染器插件
 * [FCL Renderer Plugin](https://github.com/FCL-Team/FCLRendererPlugin)
 */
object RendererPluginManager {
    private val rendererPluginList: MutableList<RendererPlugin> = mutableListOf()
    private val apkRendererPluginList: MutableList<ApkRendererPlugin> = mutableListOf()

    /**
     * 获取当前渲染器插件加载的所有渲染器
     */
    @JvmStatic
    fun getRendererList() = rendererPluginList

    /**
     * 移除某些已加载的渲染器
     */
    @JvmStatic
    fun removeRenderer(rendererPlugins: Collection<RendererPlugin>) {
        rendererPluginList.removeAll(rendererPlugins)
    }

    /**
     * @return 是可用的
     */
    @JvmStatic
    fun isAvailable(): Boolean {
        return rendererPluginList.isNotEmpty()
    }

    /**
     * 当前选择的渲染器插件所加载的渲染器
     * 根据总渲染器管理者选择的渲染器的渲染器唯一标识符进行判断
     */
    @JvmStatic
    val selectedRendererPlugin: RendererPlugin?
        get() {
            val currentRenderer = runCatching {
                Renderers.getCurrentRenderer().getUniqueIdentifier()
            }.getOrNull()
            return rendererPluginList.find { it.uniqueIdentifier == currentRenderer }
        }

    /**
     * 清除渲染器插件
     */
    fun clearPlugin() {
        rendererPluginList.clear()
        apkRendererPluginList.clear()
    }

    /**
     * 当前渲染器插件是否带有配置项（软件式插件、白名单包名）
     */
    @JvmStatic
    fun getConfigurablePluginOrNull(rendererUniqueIdentifier: String): RendererPlugin? {
        val renderer = apkRendererPluginList.find { it.uniqueIdentifier == rendererUniqueIdentifier }
        return renderer?.takeIf { it.packageName in setOf(
                "com.bzlzhh.plugin.ngg",
                "com.bzlzhh.plugin.ngg.angleless",
                "com.fcl.plugin.mobileglues"
            ) }
    }

    /**
     * 解析 ZalithLauncher、FCL 渲染器插件
     */
    fun parseApkPlugin(context: Context, info: ApplicationInfo) {
        if (info.flags and ApplicationInfo.FLAG_SYSTEM == 0) {
            val metaData = info.metaData ?: return
            if (
                metaData.getBoolean("fclPlugin", false) ||
                metaData.getBoolean("zalithRendererPlugin", false)
            ) {
                val rendererString = metaData.getString("renderer") ?: return
                val des = metaData.getString("des") ?: return
                val pojavEnvString = metaData.getString("pojavEnv") ?: return
                val nativeLibraryDir = info.nativeLibraryDir
                val renderer = rendererString.split(":")

                var rendererId: String = renderer[0]
                val envList = mutableMapOf<String, String>()
                val dlopenList = mutableListOf<String>()
                pojavEnvString.split(":").forEach { envString ->
                    if (envString.contains("=")) {
                        val stringList = envString.split("=")
                        val key = stringList[0]
                        val value = stringList[1]
                        when (key) {
                            "POJAV_RENDERER" -> rendererId = value
                            "TAG_RENDERER" -> rendererId = value
                            "DLOPEN" -> {
                                value.split(",").forEach { lib ->
                                    dlopenList.add(lib)
                                }
                            }
                            "LIB_MESA_NAME", "MESA_LIBRARY" -> envList[key] = "$nativeLibraryDir/$value"
                            else -> envList[key] = value
                        }
                    }
                }

                val packageName = info.packageName

                val plugin = ApkRendererPlugin(
                    id = rendererId,
                    displayName = des,
                    summary = context.getString(
                        R.string.settings_renderer_from_plugins,
                        runCatching {
                            context.packageManager.getApplicationLabel(info)
                        }.getOrElse {
                            context.getString(R.string.generic_unknown)
                        }
                    ),
                    minMCVer = metaData.getString("minMCVer")?.takeIf { it.isNotEmpty() && it.isNotBlank() },
                    maxMCVer = metaData.getString("maxMCVer")?.takeIf { it.isNotEmpty() && it.isNotBlank() },
                    uniqueIdentifier = packageName,
                    glName = renderer[1],
                    eglName = renderer[2].progressEglName(nativeLibraryDir),
                    path = nativeLibraryDir,
                    env = envList,
                    dlopen = dlopenList,
                    packageName = packageName
                )

                rendererPluginList.add(plugin)
                apkRendererPluginList.add(plugin)
            }
        }
    }

    private fun String.progressEglName(libPath: String): String =
        if (startsWith("/")) "$libPath$this"
        else this
}
