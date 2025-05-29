package com.movtery.zalithlauncher.components.jre

import android.content.Context
import com.movtery.zalithlauncher.components.UnpackSingleTask
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.utils.file.extractFromZip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipFile

class UnpackJnaTask(context: Context) : UnpackSingleTask(
    context = context,
    rootDir = PathManager.DIR_JNA,
    assetsDirName = "runtimes",
    fileDirName = "jna"
) {
    override suspend fun moreProgress(file: File) {
        if (file.extension == "zip") {
            withContext(Dispatchers.IO) {
                ZipFile(file).use { zip ->
                    //解压 jna 压缩包
                    zip.extractFromZip("", this@UnpackJnaTask.rootDir)
                }
                file.delete()
            }
        }
    }
}