package com.movtery.zalithlauncher.game.version.installed

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import com.movtery.zalithlauncher.BuildConfig
import com.movtery.zalithlauncher.context.GlobalContext
import com.movtery.zalithlauncher.game.path.getGameHome
import com.movtery.zalithlauncher.game.path.getVersionsHome
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.utils.getInt
import com.movtery.zalithlauncher.utils.platform.MemoryUtils
import com.movtery.zalithlauncher.utils.toBoolean
import java.io.File
import kotlin.math.min

/**
 * Minecraft 版本，由版本名称进行区分
 * @param versionName 版本名称
 * @param versionConfig 独立版本的配置
 * @param versionInfo 版本信息
 * @param isValid 版本的有效性
 */
class Version(
    private var versionName: String,
    private val versionConfig: VersionConfig,
    private val versionInfo: VersionInfo?,
    private val isValid: Boolean
): Parcelable {
    /**
     * 控制是否将当前账号视为离线账号启动游戏
     */
    var offlineAccountLogin: Boolean = false

    /**
     * 快速启动单人游戏（存档名），仅支持  1.20+  23w14a+
     */
    var quickPlaySingle: String? = null

    /**
     * @return 获取版本所属的版本文件夹
     */
    fun getVersionsFolder(): String = getVersionsHome()

    /**
     * @return 获取版本文件夹
     */
    fun getVersionPath(): File = File(getVersionsHome(), versionName)

    /**
     * @return 获取版本名称
     */
    fun getVersionName(): String = getVersionPath().name

    /**
     * 设置新的版本名称
     */
    fun setVersionName(versionName: String) {
        this.versionName = versionName
    }

    /**
     * @return 获取版本隔离配置
     */
    fun getVersionConfig() = versionConfig

    /**
     * @return 获取版本信息
     */
    fun getVersionInfo() = versionInfo

    /**
     * @return 版本描述是否可用
     */
    fun isSummaryValid(): Boolean {
        val summary = versionConfig.versionSummary
        return summary.isNotEmpty() && summary.isNotBlank()
    }

    /**
     * @return 获取版本描述
     */
    fun getVersionSummary(): String {
        if (!isValid()) throw IllegalStateException("The version is invalid!")
        return if (isSummaryValid()) versionConfig.versionSummary else versionInfo!!.getInfoString()
    }

    /**
     * @return 版本的有效性：是否存在版本JSON文件、版本文件夹是否存在
     */
    fun isValid() = isValid && getVersionPath().exists()

    /**
     * @return 是否开启了版本隔离
     */
    fun isIsolation() = versionConfig.isIsolation()

    /**
     * @return 是否跳过游戏完整性检查
     */
    fun skipGameIntegrityCheck() = versionConfig.skipGameIntegrityCheck()

    /**
     * @return 获取版本的游戏文件夹路径（若开启了版本隔离，则路径为版本文件夹）
     */
    fun getGameDir(): File {
        return if (versionConfig.isIsolation()) versionConfig.getVersionPath()
        //未开启版本隔离可以使用自定义路径，如果自定义路径为空（则为未设置），那么返回默认游戏路径（.minecraft/）
        else if (versionConfig.customPath.isNotEmpty()) File(versionConfig.customPath)
        else File(getGameHome())
    }

    private fun String.getValueOrDefault(default: String): String = this.takeIf { it.isNotEmpty() } ?: default

    fun getRenderer(): String = versionConfig.renderer.getValueOrDefault(AllSettings.renderer.getValue())

    fun getDriver(): String = versionConfig.driver.getValueOrDefault(AllSettings.vulkanDriver.getValue())

    fun getJavaRuntime(): String = versionConfig.javaRuntime

    fun getJvmArgs(): String = versionConfig.jvmArgs

    fun getCustomInfo(): String = versionConfig.customInfo.getValueOrDefault(AllSettings.versionCustomInfo.getValue())
        .replace("[zl_version]", BuildConfig.VERSION_NAME)

    fun getServerIp(): String? = versionConfig.serverIp.takeIf { it.isNotEmpty() && it.isNotBlank() }

    fun getRamAllocation(context: Context = GlobalContext): Int = versionConfig.ramAllocation.takeIf { it >= 256 }?.let {
        min(it, MemoryUtils.getMaxMemoryForSettings(context))
    } ?: AllSettings.ramAllocation.getValue()

    fun isTouchProxyEnabled(): Boolean = versionConfig.enableTouchProxy

    fun getTouchVibrateDuration(): Int? = versionConfig.touchVibrateDuration.takeIf { it >= 80 }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(versionName)
        dest.writeParcelable(versionConfig, flags)
        dest.writeParcelable(versionInfo, flags)
        dest.writeInt(isValid.getInt())
        dest.writeInt(offlineAccountLogin.getInt())
        dest.writeString(quickPlaySingle)
    }

    companion object CREATOR : Parcelable.Creator<Version> {
        override fun createFromParcel(parcel: Parcel): Version {
            val versionName = parcel.readString()!!
            val versionConfig = parcel.readParcelable<VersionConfig>(VersionConfig::class.java.classLoader)!!
            val versionInfo = parcel.readParcelable<VersionInfo?>(VersionInfo::class.java.classLoader)
            val isValid = parcel.readInt().toBoolean()
            val offlineAccount = parcel.readInt().toBoolean()
            val quickPlaySingle = parcel.readString()

            return Version(versionName, versionConfig, versionInfo, isValid).apply {
                offlineAccountLogin = offlineAccount
                this.quickPlaySingle = quickPlaySingle
            }
        }

        override fun newArray(size: Int): Array<Version?> {
            return arrayOfNulls(size)
        }
    }
}