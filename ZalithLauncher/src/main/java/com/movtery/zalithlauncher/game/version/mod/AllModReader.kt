package com.movtery.zalithlauncher.game.version.mod

import com.movtery.zalithlauncher.game.version.mod.LocalMod.Companion.isDisabled
import com.movtery.zalithlauncher.game.version.mod.ModMetadataReader.Companion.READERS
import com.movtery.zalithlauncher.utils.logging.Logger.lWarning
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.coroutineContext

class AllModReader(val modsDir: File) {
    companion object {
        /**
         * 最大并行任务数
         */
        const val PARALLELISM = 8
    }

    private val tasks = mutableListOf<ReadTask>()

    private fun scanFiles() {
        tasks.clear()
        val files = modsDir.listFiles()?.filter { !it.isDirectory } ?: return
        files.forEach { file ->
            tasks.add(ReadTask(file))
        }
    }

    /**
     * 异步读取所有模组文件
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun readAllMods(): List<RemoteMod> = withContext(Dispatchers.IO) {
        //扫描文件，封装任务
        scanFiles()

        val results = mutableListOf<RemoteMod>()
        val taskChannel = Channel<ReadTask>(Channel.UNLIMITED)

        val workers = List(PARALLELISM) {
            launch(Dispatchers.IO) {
                for (task in taskChannel) {
                    val mod = task.execute()
                    synchronized(results) {
                        results.add(mod)
                    }
                }
            }
        }

        tasks.forEach { taskChannel.send(it) }
        taskChannel.close()

        workers.forEach { it.join() }

        return@withContext results.sortedBy { it.localMod.file.name }
    }

    class ReadTask(private val file: File) {
        suspend fun execute(): RemoteMod {
            try {
                coroutineContext.ensureActive()

                val extension = if (file.isDisabled()) {
                    File(file.nameWithoutExtension).extension
                } else {
                    file.extension
                }

                return READERS[extension]?.firstNotNullOfOrNull { reader ->
                    runCatching {
                        RemoteMod(
                            localMod = reader.fromLocal(file)
                        )
                    }.getOrNull()
                    //返回null，继续使用下一个解析器
                } ?: throw IllegalArgumentException("No matching reader for extension: $extension")
            } catch (e: Exception) {
                when (e) {
                    is CancellationException -> throw e
                    else -> {
                        lWarning("Failed to read mod: $file", e)
                        return RemoteMod(
                            localMod = LocalMod.createNotMod(file)
                        )
                    }
                }
            }
        }
    }
}