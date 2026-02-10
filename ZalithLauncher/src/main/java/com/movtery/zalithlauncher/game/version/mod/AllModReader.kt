/*
 * Zalith Launcher 2
 * Copyright (C) 2025 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.game.version.mod

import com.movtery.zalithlauncher.utils.logging.Logger.lWarning
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

const val READER_PARALLELISM = 8

class AllModReader(val modsDir: File) {
    val resultsMutex = Mutex()

    private fun scanFiles(): List<ReadTask> {
        val files = modsDir.listFiles()?.filter { !it.isDirectory } ?: return emptyList()
        return files.map { file ->
            ReadTask(file)
        }
    }

    /**
     * 异步读取所有模组文件
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun readAllMods(): List<RemoteMod> = withContext(Dispatchers.IO) {
        //扫描文件，封装任务
        val tasks = scanFiles()

        val results = mutableListOf<RemoteMod>()
        val taskChannel = Channel<ReadTask>(Channel.UNLIMITED)

        val workers = List(READER_PARALLELISM) {
            launch(Dispatchers.IO) {
                for (task in taskChannel) {
                    val mod = task.execute()
                    resultsMutex.withLock {
                        results.add(mod)
                    }
                }
            }
        }

        tasks.forEach { taskChannel.send(it) }
        taskChannel.close()

        workers.joinAll()

        return@withContext results.sortedBy { it.localMod.file.name }
    }

    class ReadTask(private val file: File) {
        suspend fun execute(): RemoteMod {
            try {
                currentCoroutineContext().ensureActive()

                val extension = if (file.isDisabled()) {
                    File(file.nameWithoutExtension).extension
                } else {
                    file.extension
                }

                return MOD_READERS[extension]?.firstNotNullOfOrNull { reader ->
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
                            localMod = createNotMod(file)
                        )
                    }
                }
            }
        }
    }
}