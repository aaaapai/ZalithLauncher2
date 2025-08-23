package com.movtery.zalithlauncher.game.launch

import android.content.Context
import android.os.Build
import android.os.FileObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.movtery.zalithlauncher.context.copyAssetFile
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.utils.logging.Logger.lError
import com.movtery.zalithlauncher.utils.logging.Logger.lWarning
import com.movtery.zalithlauncher.utils.string.StringUtils.Companion.splitPreservingQuotes
import org.lwjgl.glfw.CallbackBridge.windowHeight
import org.lwjgl.glfw.CallbackBridge.windowWidth
import java.io.File
import java.util.concurrent.ConcurrentHashMap

object MCOptions {
    private val lock = Any()
    private val parameterMap = ConcurrentHashMap<String, String>()
    private var fileObserver: FileObserver? = null
    private lateinit var version: Version

    /**
     * GUI 缩放尺寸
     */
    var guiScale by mutableIntStateOf(0)
        private set

    /**
     * 初始化 Minecraft 选项配置
     */
    fun setup(context: Context, version: Version) {
        this.version = version
        synchronized(lock) {
            parameterMap.clear()
            fileObserver?.stopWatching()
            setupFileStructure(context)
            loadInternal()
            setupFileObserver()
        }
    }

    private fun setupFileStructure(context: Context) {
        getOptionsFile().apply {
            parentFile?.takeIf { !it.exists() }?.mkdirs()
            if (!exists()) createWithDefaults(context)
        }
    }

    private fun File.createWithDefaults(context: Context) {
        runCatching {
            context.copyAssetFile(
                "game/options.txt",
                parentFile?.absolutePath ?: return,
                false
            )
        }.onFailure {
            lWarning("Failed to unpack options.txt!", it)
        }
    }

    private fun loadInternal() {
        val optionsFile = getOptionsFile()
        runCatching {
            val newMap = optionsFile.readLines()
                .mapNotNull { line ->
                    line.indexOf(':').takeIf { it > 0 }?.let { idx ->
                        line.substring(0, idx) to line.substring(idx + 1)
                    }
                }.toMap()

            parameterMap.clear()
            parameterMap.putAll(newMap)

            val guiScale = get("guiScale")?.toIntOrNull() ?: 0
            val dynamicScale = calculateDynamicScale()
            this.guiScale = if (guiScale == 0 || dynamicScale < guiScale) dynamicScale else guiScale
        }.onFailure {
            lWarning("Failed to load options!", it)
        }
    }

    fun set(key: String, value: String) = parameterMap.put(key, value)

    fun set(key: String, value: List<String>) {
        set(key, value.joinToString(prefix = "[", postfix = "]") { "\"$it\"" })
    }

    fun get(key: String): String? = parameterMap[key]

    fun getAsList(key: String): List<String> {
        val raw = get(key) ?: return emptyList()

        return raw
            .removeSurrounding("[", "]")
            .takeIf { it.isNotBlank() }
            ?.splitPreservingQuotes(',')
            ?.map { it.trim() }
            ?: emptyList()
    }

    fun containsKey(key: String): Boolean = parameterMap.containsKey(key)

    fun save() {
        synchronized(lock) {
            getOptionsFile().takeIf { it.exists() }?.let { file ->
                try {
                    fileObserver?.stopWatching()
                    writeFileAtomically(file)
                } finally {
                    fileObserver?.startWatching()
                }
            }
        }
    }

    private fun writeFileAtomically(targetFile: File) {
        val tempFile = File(targetFile.parent, "${targetFile.name}.tmp").apply {
            deleteOnExit()
        }

        runCatching {
            tempFile.writeText(
                parameterMap.entries.joinToString("\n") { "${it.key}:${it.value}" }
            )
            tempFile.renameTo(targetFile)
        }.onFailure {
            lError("Failed to save options.txt!", it)
            tempFile.delete()
        }
    }

    private fun calculateDynamicScale() = minOf(
        windowWidth / 320,
        windowHeight / 240
    ).coerceAtLeast(1)

    private fun getOptionsFile() = File(version.getGameDir(), "options.txt")

    private fun setupFileObserver() {
        fileObserver?.stopWatching()
        fileObserver = createPlatformFileObserver().apply {
            startWatching()
        }
    }

    @Suppress("DEPRECATION")
    private fun createPlatformFileObserver(): FileObserver {
        val observeTarget = getOptionsFile()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            object : FileObserver(observeTarget, MODIFY) {
                override fun onEvent(event: Int, path: String?) {
                    if (event and MODIFY != 0) {
                        synchronized(lock) {
                            loadInternal()
                        }
                    }
                }
            }
        } else {
            object : FileObserver(observeTarget.absolutePath, MODIFY) {
                override fun onEvent(event: Int, path: String?) {
                    if (event and MODIFY != 0) {
                        synchronized(lock) {
                            loadInternal()
                        }
                    }
                }
            }
        }
    }
}