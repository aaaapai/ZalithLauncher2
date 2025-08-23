package com.movtery.zalithlauncher.game.version.installed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.path.getVersionsHome
import com.movtery.zalithlauncher.game.version.installed.VersionType.Companion.getVersionType
import com.movtery.zalithlauncher.game.version.installed.utils.VersionInfoUtils
import com.movtery.zalithlauncher.info.InfoDistributor
import com.movtery.zalithlauncher.utils.logging.Logger.lError
import com.movtery.zalithlauncher.utils.logging.Logger.lInfo
import com.movtery.zalithlauncher.utils.logging.Logger.lWarning
import com.movtery.zalithlauncher.utils.string.compareChar
import com.movtery.zalithlauncher.utils.string.compareVersion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.apache.commons.io.FileUtils
import java.io.File

object VersionsManager {
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _versions = MutableStateFlow<List<Version>>(emptyList())
    private val _vanillaVersions = MutableStateFlow<List<Version>>(emptyList())
    private val _modloaderVersions = MutableStateFlow<List<Version>>(emptyList())
    val versions: StateFlow<List<Version>> = _versions
    val vanillaVersions: StateFlow<List<Version>> = _vanillaVersions
    val modloaderVersions: StateFlow<List<Version>> = _modloaderVersions

    fun allVersionsCount() = _versions.value.size
    fun vanillaVersionsCount() = _vanillaVersions.value.size
    fun modloaderVersionsCount() = _modloaderVersions.value.size

    /**
     * 当前的游戏信息
     */
    var currentGameInfo by mutableStateOf<CurrentGameInfo?>(null)
        private set

    /**
     * 当前的版本
     */
    var currentVersion by mutableStateOf<Version?>(null)
        private set

    private var currentJob: Job? = null

    /**
     * 是否正在刷新版本
     */
    var isRefreshing by mutableStateOf(false)
        private set

    /**
     * 检查版本是否已经存在
     */
    fun isVersionExists(versionName: String, checkJson: Boolean = false): Boolean {
        val folder = File(getVersionsHome(), versionName)
        //保证版本文件夹存在的同时，也应保证其版本json文件存在
        return if (checkJson) File(folder, "${folder.name}.json").exists()
        else folder.exists()
    }

    fun refresh() {
        currentJob?.cancel()
        currentJob = scope.launch {
            isRefreshing = true

            _versions.update { emptyList() }
            _vanillaVersions.update { emptyList() }
            _modloaderVersions.update { emptyList() }

            val newVersions = mutableListOf<Version>()
            File(getVersionsHome()).listFiles()?.forEach { versionFile ->
                runCatching {
                    processVersionFile(versionFile)
                }.getOrNull()?.let {
                    newVersions.add(it)
                }
            }

            newVersions.sortWith { o1, o2 ->
                val thisVer = o1.getVersionInfo()?.minecraftVersion ?: o1.getVersionName()
                var sort = -thisVer.compareVersion(
                    o2.getVersionInfo()?.minecraftVersion ?: o2.getVersionName()
                )
                if (sort == 0) sort =
                    compareChar(o1.getVersionName(), o2.getVersionName())
                sort
            }

            _versions.update { newVersions.toList() }
            _vanillaVersions.update { newVersions.filter { ver -> ver.versionType == VersionType.VANILLA } }
            _modloaderVersions.update { newVersions.filter { ver -> ver.versionType == VersionType.MODLOADERS } }

            currentGameInfo = CurrentGameInfo.refreshCurrentInfo()
            refreshCurrentVersion()

            isRefreshing = false
        }
    }

    private fun processVersionFile(versionFile: File): Version? {
        if (versionFile.exists() && versionFile.isDirectory) {
            var isVersion = false

            //通过判断是否存在版本的.json文件，来确定其是否为一个版本
            val jsonFile = File(versionFile, "${versionFile.name}.json")
            val versionInfo = if (jsonFile.exists() && jsonFile.isFile) {
                VersionInfoUtils.parseJson(jsonFile)?.also {
                    //如果解析失败了，可能不是标准版本
                    //保险起见，只有解析成功了的版本，才会被判定为有效版本
                    isVersion = true
                }
            } else {
                null
            }

            val versionConfig = VersionConfig.parseConfig(versionFile)

            val version = Version(
                versionFile.name,
                versionConfig,
                versionInfo,
                isVersion,
                versionInfo.getVersionType()
            )

            lInfo(
                "Identified and added version: ${version.getVersionName()}, " +
                        "Path: (${version.getVersionPath()}), " +
                        "Info: ${version.getVersionInfo()?.getInfoString()}"
            )

            return version
        }
        return null
    }

    private fun refreshCurrentVersion() {
        currentVersion = run {
            if (_versions.value.isEmpty()) return@run null

            fun returnVersionByFirst(): Version? {
                return _versions.value.find { it.isValid() }?.apply {
                    //确保版本有效
                    saveCurrentVersion(getVersionName())
                }
            }

            runCatching {
                val versionString = currentGameInfo!!.version
                getVersion(versionString) ?: returnVersionByFirst()
            }.onFailure { e ->
                lWarning("The current version information has not been initialized yet.", e)
            }.getOrElse {
                returnVersionByFirst()
            }
        }
    }

    private fun getVersion(name: String?): Version? {
        name?.let { versionName ->
            return _versions.value.find { it.getVersionName() == versionName }?.takeIf { it.isValid() }
        }
        return null
    }

    /**
     * @return 通过版本名，判断其版本是否存在
     */
    fun checkVersionExistsByName(versionName: String?) =
        versionName?.let { name -> _versions.value.any { it.getVersionName() == name } } ?: false

    /**
     * @return 获取 Zalith 启动器版本标识文件夹
     */
    fun getZalithVersionPath(version: Version) = File(version.getVersionPath(), InfoDistributor.LAUNCHER_IDENTIFIER)

    /**
     * @return 通过目录获取 Zalith 启动器版本标识文件夹
     */
    fun getZalithVersionPath(folder: File) = File(folder, InfoDistributor.LAUNCHER_IDENTIFIER)

    /**
     * @return 通过名称获取 Zalith 启动器版本标识文件夹
     */
    fun getZalithVersionPath(name: String) = File(getVersionPath(name), InfoDistributor.LAUNCHER_IDENTIFIER)

    /**
     * @return 获取当前版本设置的图标
     */
    fun getVersionIconFile(version: Version) = File(getZalithVersionPath(version), "VersionIcon.png")

    /**
     * @return 通过目录获取 Zalith 启动器版本标识文件夹
     */
    fun getVersionIconFile(folder: File) = File(getZalithVersionPath(folder), "VersionIcon.png")

    /**
     * @return 通过名称获取当前版本设置的图标
     */
    fun getVersionIconFile(name: String) = File(getZalithVersionPath(name), "VersionIcon.png")

    /**
     * @return 通过名称获取版本的文件夹路径
     */
    fun getVersionPath(name: String) = File(getVersionsHome(), name)

    /**
     * 保存当前选择的版本
     */
    fun saveCurrentVersion(versionName: String) {
        runCatching {
            currentGameInfo!!.apply {
                version = versionName
                saveCurrentInfo()
            }
            refreshCurrentVersion()
        }.onFailure { e ->
            lError("An exception occurred while saving the currently selected version information.", e)
        }
    }

    @Composable
    fun validateVersionName(
        newName: String,
        versionInfo: VersionInfo?,
        onError: (message: String) -> Unit
    ): Boolean {
        return when {
            isVersionExists(newName, true) -> {
                onError(stringResource(R.string.versions_manage_install_exists))
                true
            }
            versionInfo?.loaderInfo?.let {
                //如果这个版本是有ModLoader加载器信息的，则不允许修改为与原版名称一致的名称，防止冲突
                newName == versionInfo.minecraftVersion
            } ?: false -> {
                onError(stringResource(R.string.versions_manage_install_cannot_use_mc_name))
                true
            }
            else -> false
        }
    }

    /**
     * 重命名当前版本，但并不会在这里对即将重命名的名称，进行非法性判断
     */
    fun renameVersion(version: Version, name: String) {
        val currentVersionName = currentVersion?.getVersionName()
        //如果当前的版本是即将被重命名的版本，那么就把将要重命名的名字设置为当前版本
        val saveToCurrent = version.getVersionName() == currentVersionName

        val versionFolder = version.getVersionPath()
        val renameFolder = File(getVersionsHome(), name)

        //不管重命名之后的文件夹是什么，只要这个文件夹存在，那么就必须删除
        //否则将出现问题
        FileUtils.deleteQuietly(renameFolder)

        val originalName = versionFolder.name

        versionFolder.renameTo(renameFolder)

        val versionJsonFile = File(renameFolder, "$originalName.json")
        val versionJarFile = File(renameFolder, "$originalName.jar")
        val renameJsonFile = File(renameFolder, "$name.json")
        val renameJarFile = File(renameFolder, "$name.jar")

        versionJsonFile.renameTo(renameJsonFile)
        versionJarFile.renameTo(renameJarFile)

        FileUtils.deleteQuietly(versionFolder)

        if (saveToCurrent) {
            //设置并刷新当前版本
            saveCurrentVersion(name)
        }

        refresh()
    }

    /**
     * 将选中的版本复制为一个新的版本
     * @param version 选中的版本
     * @param name 新的版本的名称
     * @param copyAllFile 是否复制全部文件
     */
    fun copyVersion(version: Version, name: String, copyAllFile: Boolean) {
        val versionsFolder = version.getVersionsFolder()
        val newVersion = File(versionsFolder, name)

        val originalName = version.getVersionName()

        //新版本的json与jar文件
        val newJsonFile = File(newVersion, "$name.json")
        val newJarFile = File(newVersion, "$name.jar")

        val originalVersionFolder = version.getVersionPath()
        if (copyAllFile) {
            //启用复制所有文件时，直接将原文件夹整体复制到新版本
            FileUtils.copyDirectory(originalVersionFolder, newVersion)
            //重命名json、jar文件
            val jsonFile = File(newVersion, "$originalName.json")
            val jarFile = File(newVersion, "$originalName.jar")
            if (jsonFile.exists()) jsonFile.renameTo(newJsonFile)
            if (jarFile.exists()) jarFile.renameTo(newJarFile)
        } else {
            //不复制所有文件时，仅复制并重命名json、jar文件
            val originalJsonFile = File(originalVersionFolder, "$originalName.json")
            val originalJarFile = File(originalVersionFolder, "$originalName.jar")
            newVersion.mkdirs()
            // versions/1.21.3/1.21.3.json -> versions/name/name.json
            if (originalJsonFile.exists()) originalJsonFile.copyTo(newJsonFile)
            // versions/1.21.3/1.21.3.jar -> versions/name/name.jar
            if (originalJarFile.exists()) originalJarFile.copyTo(newJarFile)
        }

        //保存版本配置文件
        version.getVersionConfig().copy().let { config ->
            config.setVersionPath(newVersion)
            config.isolationType = SettingState.ENABLE
            config.saveWithThrowable()
        }

        refresh()
    }

    /**
     * 删除版本
     */
    fun deleteVersion(version: Version) {
        FileUtils.deleteQuietly(version.getVersionPath())
        refresh()
    }
}