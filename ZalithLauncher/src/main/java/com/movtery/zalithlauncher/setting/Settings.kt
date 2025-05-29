package com.movtery.zalithlauncher.setting

import androidx.annotation.CheckResult
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.setting.unit.AbstractSettingUnit
import com.movtery.zalithlauncher.utils.logging.Logger.lError
import org.apache.commons.io.FileUtils
import java.lang.reflect.Type
import java.util.concurrent.ConcurrentHashMap

class Settings {
    companion object {
        private val GSON: Gson = GsonBuilder().disableHtmlEscaping().create()

        private val settingsLock = Any()
        private var settingsMap = ConcurrentHashMap<String, SettingAttribute>()

        private fun refreshSettingsMap(): Map<String, SettingAttribute> {
            return PathManager.FILE_SETTINGS.takeIf { it.exists() }?.let { file ->
                try {
                    val jsonString = file.readText()
                    val listType: Type = object : TypeToken<List<SettingAttribute>>() {}.type
                    GSON.fromJson<List<SettingAttribute>>(jsonString, listType)
                        .associateBy { it.key }
                } catch (e: Exception) {
                    lError("Failed to refresh settings", e)
                    emptyMap()
                }
            } ?: emptyMap()
        }

        /**
         * 刷新启动器的所有设置项
         */
        @Synchronized
        fun refreshSettings() {
            settingsMap = ConcurrentHashMap(refreshSettingsMap())
        }
    }

    class Manager private constructor() {
        companion object {
            /**
             * 在启动器设置中获取键对应的值
             */
            fun <T> getValue(key: String, defaultValue: T, parser: (String) -> T?): T {
                return settingsMap[key]?.value?.let { parser(it) } ?: defaultValue
            }

            /**
             * 检查启动器设置中，是否存在某个键
             */
            @JvmStatic
            fun contains(key: String): Boolean {
                return settingsMap.containsKey(key)
            }

            /**
             * 在启动器设置中存入键值
             * @param unit 设置单元
             */
            @JvmStatic
            @CheckResult
            fun <V> put(unit: AbstractSettingUnit<V>, value: V) = unit.put(value)
        }

        class SettingBuilder {
            private val valueMap = ConcurrentHashMap<String, Any>()

            /**
             * 在启动器设置中存入键值
             * 不开放单独设置键、值
             */
            @CheckResult
            private fun put(key: String, value: Any): SettingBuilder {
                valueMap[key] = value
                return this
            }

            /**
             * 在启动器设置中存入键值
             * @param unit 设置单元
             */
            @CheckResult
            fun put(unit: AbstractSettingUnit<*>, value: Any): SettingBuilder {
                return put(unit.key, value)
            }

            fun save() {
                val settingsFile = PathManager.FILE_SETTINGS
                val newSettings = ConcurrentHashMap(settingsMap)

                valueMap.forEach { (key, value) ->
                    newSettings[key] = SettingAttribute(key, value.toString())
                }

                synchronized(settingsLock) {
                    runCatching {
                        if (!settingsFile.exists() && !settingsFile.createNewFile()) {
                            throw IllegalStateException("Failed to create settings file")
                        }

                        val settingsList = newSettings.values.toList()
                        val json = GSON.toJson(settingsList)
                        FileUtils.write(settingsFile, json, Charsets.UTF_8)
                        refreshSettings()
                    }.onFailure { e ->
                        lError("Failed to save launcher settings!", e)
                    }
                }
            }
        }
    }

    private class SettingAttribute(
        var key: String = "",
        var value: String? = null
    )
}