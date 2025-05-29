package com.movtery.zalithlauncher.components

import android.content.Context
import android.content.res.AssetManager
import com.movtery.zalithlauncher.context.copyAssetFile
import com.movtery.zalithlauncher.utils.file.child
import com.movtery.zalithlauncher.utils.file.readString
import com.movtery.zalithlauncher.utils.logging.Logger.lInfo
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

abstract class UnpackSingleTask(
    val context: Context,
    val rootDir: File,
    val assetsDirName: String,
    val fileDirName: String,
) : AbstractUnpackTask() {
    private lateinit var am: AssetManager
    private lateinit var versionFile: File
    private lateinit var input: InputStream
    private var isCheckFailed: Boolean = false

    init {
        runCatching {
            am = context.assets
            versionFile = File("$rootDir/$fileDirName/version")
            input = am.open("$assetsDirName/$fileDirName/version")
        }.getOrElse {
            isCheckFailed = true
        }
    }

    fun isCheckFailed() = isCheckFailed

    override fun isNeedUnpack(): Boolean {
        if (isCheckFailed) return false

        if (!versionFile.exists()) {
            requestEmptyParentDir(versionFile)
            lInfo("$fileDirName: Pack was installed manually, or does not exist...")
            return true
        } else {
            val fis = FileInputStream(versionFile)
            val release1 = input.readString()
            val release2 = fis.readString()
            if (release1 != release2) {
                requestEmptyParentDir(versionFile)
                return true
            } else {
                lInfo("$fileDirName: Pack is up-to-date with the launcher, continuing...")
                return false
            }
        }
    }

    override suspend fun run() {
        FileUtils.deleteDirectory(rootDir)

        val fileList = am.list("$assetsDirName/$fileDirName")
        for (fileName in fileList!!) {
            val file = rootDir.child(fileDirName, fileName)
            context.copyAssetFile(
                "$assetsDirName/$fileDirName/$fileName",
                file,
                true
            )
            moreProgress(file)
        }
    }

    /**
     * 执行更多操作
     */
    open suspend fun moreProgress(file: File) {}

    private fun requestEmptyParentDir(file: File) {
        file.parentFile!!.apply {
            if (exists() and isDirectory) {
                FileUtils.deleteDirectory(this)
            }
            mkdirs()
        }
    }
}