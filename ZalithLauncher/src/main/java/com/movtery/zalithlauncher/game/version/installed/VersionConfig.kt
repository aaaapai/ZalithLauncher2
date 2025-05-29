package com.movtery.zalithlauncher.game.version.installed

import android.os.Parcel
import android.os.Parcelable
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.version.installed.VersionsManager.getZalithVersionPath
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.utils.GSON
import com.movtery.zalithlauncher.utils.getInt
import com.movtery.zalithlauncher.utils.logging.Logger.lError
import com.movtery.zalithlauncher.utils.logging.Logger.lInfo
import com.movtery.zalithlauncher.utils.string.StringUtils.Companion.getStringNotNull
import com.movtery.zalithlauncher.utils.toBoolean
import java.io.File
import java.io.FileWriter

class VersionConfig(private var versionPath: File) : Parcelable {
    var isolationType: SettingState = SettingState.FOLLOW_GLOBAL
        get() = getSettingStateNotNull(field)
    var skipGameIntegrityCheck: SettingState = SettingState.FOLLOW_GLOBAL
        get() = getSettingStateNotNull(field)
    var javaRuntime: String = ""
        get() = getStringNotNull(field)
    var jvmArgs: String = ""
        get() = getStringNotNull(field)
    var renderer: String = ""
        get() = getStringNotNull(field)
    var driver: String = ""
        get() = getStringNotNull(field)
    var control: String = ""
        get() = getStringNotNull(field)
    var customPath: String = ""
        get() = getStringNotNull(field)
    var customInfo: String = ""
        get() = getStringNotNull(field)
    var versionSummary: String = ""
        get() = getStringNotNull(field)
    var serverIp: String = ""
        get() = getStringNotNull(field)
    var ramAllocation: Int = -1
    var enableTouchProxy: Boolean = false
    var touchVibrateDuration: Int = 100

    constructor(
        filePath: File,
        isolationType: SettingState = SettingState.FOLLOW_GLOBAL,
        skipGameIntegrityCheck: SettingState = SettingState.FOLLOW_GLOBAL,
        javaRuntime: String = "",
        jvmArgs: String = "",
        renderer: String = "",
        driver: String = "",
        control: String = "",
        customPath: String = "",
        customInfo: String = "",
        versionSummary: String = "",
        serverIp: String = "",
        ramAllocation: Int = -1,
        enableTouchProxy: Boolean = false,
        touchVibrateDuration: Int = 100
    ) : this(filePath) {
        this.isolationType = isolationType
        this.skipGameIntegrityCheck = skipGameIntegrityCheck
        this.javaRuntime = javaRuntime
        this.jvmArgs = jvmArgs
        this.renderer = renderer
        this.driver = driver
        this.control = control
        this.customPath = customPath
        this.customInfo = customInfo
        this.versionSummary = versionSummary
        this.serverIp = serverIp
        this.ramAllocation = ramAllocation
        this.enableTouchProxy = enableTouchProxy
        this.touchVibrateDuration = touchVibrateDuration
    }

    fun copy(): VersionConfig = VersionConfig(
        versionPath,
        getSettingStateNotNull(isolationType),
        getSettingStateNotNull(skipGameIntegrityCheck),
        getStringNotNull(javaRuntime),
        getStringNotNull(jvmArgs),
        getStringNotNull(renderer),
        getStringNotNull(driver),
        getStringNotNull(control),
        getStringNotNull(customPath),
        getStringNotNull(customInfo),
        getStringNotNull(versionSummary),
        getStringNotNull(serverIp),
        ramAllocation,
        enableTouchProxy,
        touchVibrateDuration
    )

    fun save() {
        runCatching {
            saveWithThrowable()
        }.onFailure { e ->
            lError("An exception occurred while saving the version configuration.", e)
        }
    }

    @Throws(Throwable::class)
    fun saveWithThrowable() {
        val zalithVersionPath = getZalithVersionPath(versionPath)
        val configFile = File(zalithVersionPath, "version.config")
        if (!zalithVersionPath.exists()) zalithVersionPath.mkdirs()

        FileWriter(configFile, false).use {
            val json = GSON.toJson(this)
            it.write(json)
        }
        lInfo("Saved version configuration: $this")
    }

    fun getVersionPath() = versionPath

    fun setVersionPath(versionPath: File) {
        this.versionPath = versionPath
    }

    fun isIsolation(): Boolean = isolationType.toBoolean(AllSettings.versionIsolation.getValue())

    fun skipGameIntegrityCheck(): Boolean = skipGameIntegrityCheck.toBoolean(AllSettings.skipGameIntegrityCheck.getValue())

    private fun SettingState.toBoolean(global: Boolean) = when(getSettingStateNotNull(this)) {
        SettingState.FOLLOW_GLOBAL -> global
        SettingState.ENABLE -> true
        SettingState.DISABLE -> false
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.run {
            writeString(versionPath.absolutePath)
            writeInt(getSettingStateNotNull(isolationType).ordinal)
            writeInt(getSettingStateNotNull(skipGameIntegrityCheck).ordinal)
            writeString(getStringNotNull(javaRuntime))
            writeString(getStringNotNull(jvmArgs))
            writeString(getStringNotNull(renderer))
            writeString(getStringNotNull(driver))
            writeString(getStringNotNull(control))
            writeString(getStringNotNull(customPath))
            writeString(getStringNotNull(customInfo))
            writeString(getStringNotNull(versionSummary))
            writeString(getStringNotNull(serverIp))
            writeInt(ramAllocation)
            writeInt(enableTouchProxy.getInt())
            writeInt(touchVibrateDuration)
        }
    }

    companion object CREATOR : Parcelable.Creator<VersionConfig> {
        override fun createFromParcel(parcel: Parcel): VersionConfig {
            val versionPath = File(parcel.readString().orEmpty())
            val isolationType = SettingState.entries.getOrNull(parcel.readInt()) ?: SettingState.FOLLOW_GLOBAL
            val skipGameIntegrityCheck = SettingState.entries.getOrNull(parcel.readInt()) ?: SettingState.FOLLOW_GLOBAL
            val javaRuntime = parcel.readString().orEmpty()
            val jvmArgs = parcel.readString().orEmpty()
            val renderer = parcel.readString().orEmpty()
            val driver = parcel.readString().orEmpty()
            val control = parcel.readString().orEmpty()
            val customPath = parcel.readString().orEmpty()
            val customInfo = parcel.readString().orEmpty()
            val versionSummary = parcel.readString().orEmpty()
            val serverIp = parcel.readString().orEmpty()
            val ramAllocation = parcel.readInt()
            val enableTouchProxy = parcel.readInt().toBoolean()
            val touchVibrateDuration = parcel.readInt()

            return VersionConfig(
                versionPath,
                isolationType,
                skipGameIntegrityCheck,
                javaRuntime,
                jvmArgs,
                renderer,
                driver,
                control,
                customPath,
                customInfo,
                versionSummary,
                serverIp,
                ramAllocation,
                enableTouchProxy,
                touchVibrateDuration
            )
        }

        override fun newArray(size: Int): Array<VersionConfig?> {
            return arrayOfNulls(size)
        }

        fun parseConfig(versionPath: File): VersionConfig {
            val configFile = File(getZalithVersionPath(versionPath), "version.config")

            return runCatching getConfig@{
                when {
                    configFile.exists() -> {
                        //读取此文件的内容，并解析为VersionConfig
                        val config = GSON.fromJson(configFile.readText(), VersionConfig::class.java)
                        config.setVersionPath(versionPath)
                        config
                    }
                    else -> createNewConfig(versionPath)
                }
            }.onFailure {  e ->
                lError("An exception occurred while parsing the version configuration.", e)
            }.getOrElse {
                createNewConfig(versionPath)
            }
        }

        private fun createNewConfig(versionPath: File): VersionConfig {
            val config = VersionConfig(versionPath)
            return config.apply { save() }
        }

        fun createIsolation(versionPath: File): VersionConfig {
            val config = VersionConfig(versionPath)
            config.isolationType = SettingState.ENABLE
            return config
        }
    }
}

enum class SettingState(val textRes: Int) {
    FOLLOW_GLOBAL(R.string.generic_follow_global),
    ENABLE(R.string.generic_enable),
    DISABLE(R.string.generic_disable)
}

private fun getSettingStateNotNull(type: SettingState?) = type ?: SettingState.FOLLOW_GLOBAL
