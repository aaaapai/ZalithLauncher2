package com.movtery.zalithlauncher.ui.screens.game.elements

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import com.movtery.zalithlauncher.bridge.LoggerBridge
import com.movtery.zalithlauncher.setting.AllSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Collections

@Composable
fun LogBox(
    enableLog: Boolean,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberLazyListState()

    val logList = remember { mutableStateListOf<String>() }
    val buffer = remember { Collections.synchronizedList(mutableListOf<String>()) }

    val config = remember {
        object {
            /** 缓冲区刷新间隔，单位：ms */
            val BUFFER_FLUSH_INTERVAL: Long = AllSettings.logBufferFlushInterval.getValue().toLong()
        }
    }

    LaunchedEffect(enableLog) {
        if (enableLog) {
            val scrollChannel = Channel<Unit>(capacity = 100)

            LoggerBridge.setListener { log ->
                synchronized(buffer) {
                    buffer.add(log)
                }
            }

            launch(Dispatchers.Default) {
                val mutex = Mutex()
                while (isActive) {
                    delay(config.BUFFER_FLUSH_INTERVAL)

                    mutex.withLock {
                        synchronized(buffer) {
                            if (buffer.isNotEmpty()) {
                                logList.addAll(buffer)
                                buffer.clear()
                                //尝试进行滚动
                                scrollChannel.trySend(Unit)
                            }
                        }
                    }
                }
            }

            //自动滚动部分
            launch(Dispatchers.Main) {
                scrollChannel.consumeAsFlow().collect {
                    if (logList.isNotEmpty()) {
                        scrollState.animateScrollToItem(logList.lastIndex)
                    }
                }
            }
        } else {
            LoggerBridge.setListener(null)
            logList.clear()
            buffer.clear()
        }
    }

    if (enableLog) {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            LogList(
                modifier = Modifier.fillMaxSize(),
                logs = logList,
                scrollState = scrollState
            )
        }
    }
}

@Composable
private fun LogList(
    modifier: Modifier = Modifier,
    logs: List<String>,
    scrollState: LazyListState
) {
    LazyColumn(
        modifier = modifier,
        state = scrollState
    ) {
        items(logs) { log ->
            Text(
                text = log,
                modifier = Modifier.fillParentMaxWidth(),
                fontSize = TextUnit(AllSettings.logTextSize.getValue().toFloat(), TextUnitType.Sp)
            )
        }
    }
}