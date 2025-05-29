package com.movtery.zalithlauncher.context

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.setting.Settings
import com.movtery.zalithlauncher.setting.loadAllSettings
import com.movtery.zalithlauncher.utils.file.ensureParentDirectory
import com.movtery.zalithlauncher.utils.file.readString
import com.movtery.zalithlauncher.utils.logging.Logger.lWarning
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.properties.Delegates

var GlobalContext by Delegates.notNull<Context>()

fun refreshContext(context: Context) {
    PathManager.refreshPaths(context)
    Settings.refreshSettings()
    loadAllSettings(context)
}

fun getContextWrapper(context: Context): ContextWrapper {
    refreshContext(context)
    return ContextWrapper(context)
}

fun Context.readAssetFile(filePath: String): String {
    return assets.open(filePath).use { it.readString() }
}

fun Context.getFileName(uri: Uri): String? {
    return runCatching {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null

            cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it != -1 }?.let { columnIndex ->
                cursor.getString(columnIndex)
            }
        } ?: uri.lastPathSegment
    }.getOrNull() ?: uri.lastPathSegment
}

@Throws(IOException::class)
fun Context.copyAssetFile(fileName: String, output: String, overwrite: Boolean) {
    this.copyAssetFile(fileName, output, File(fileName).name, overwrite)
}

@Throws(IOException::class)
fun Context.copyAssetFile(
    fileName: String,
    output: String,
    outputName: String,
    overwrite: Boolean
) {
    this.copyAssetFile(fileName, File(output, outputName), overwrite)
}

@Throws(IOException::class)
fun Context.copyAssetFile(
    fileName: String,
    output: File,
    overwrite: Boolean
) {
    val destinationFile = output.ensureParentDirectory()
    if (!destinationFile.exists() || overwrite) {
        assets.open(fileName).use { inputStream ->
            FileOutputStream(destinationFile).use { outputStream ->
                IOUtils.copy(inputStream, outputStream)
            }
        }
    }
}

@Throws(IOException::class)
fun Context.copyLocalFile(
    uri: Uri,
    outputFile: File
) {
    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    try {
        contentResolver.takePersistableUriPermission(uri, flags)
    } catch (e: SecurityException) {
        lWarning("Failed to take persistable permission for URI: $uri", e)
    }

    if (outputFile.parentFile?.exists() != true && outputFile.parentFile?.mkdirs() != true) {
        lWarning("Failed to create parent directories for output file.")
    }
    if (!outputFile.exists() && !outputFile.createNewFile()) {
        lWarning("Unable to manually create file when importing from URI to local storage.")
    }
    contentResolver.openInputStream(uri).use { inputStream ->
        FileUtils.copyToFile(inputStream, outputFile)
    }
}