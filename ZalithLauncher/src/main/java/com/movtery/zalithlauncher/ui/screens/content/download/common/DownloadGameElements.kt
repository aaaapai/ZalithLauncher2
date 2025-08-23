package com.movtery.zalithlauncher.ui.screens.content.download.common

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.coroutine.Task
import com.movtery.zalithlauncher.coroutine.TaskState
import com.movtery.zalithlauncher.game.download.game.GameDownloadInfo
import com.movtery.zalithlauncher.game.download.game.GameInstallTask
import com.movtery.zalithlauncher.ui.components.MarqueeText
import com.movtery.zalithlauncher.utils.animation.getAnimateTween

/** 游戏安装状态操作 */
sealed interface GameInstallOperation {
    data object None : GameInstallOperation
    /** 开始安装 */
    data class Install(val info: GameDownloadInfo) : GameInstallOperation
    /** 警告通知权限，可以无视，并直接开始安装 */
    data class WarningForNotification(val info: GameDownloadInfo) : GameInstallOperation
    /** 游戏安装出现异常 */
    data class Error(val th: Throwable) : GameInstallOperation
    /** 游戏已成功安装 */
    data object Success : GameInstallOperation
}

@Composable
fun GameInstallingDialog(
    title: String,
    tasks: List<GameInstallTask>,
    onCancel: () -> Unit = {}
) {
    Dialog(onDismissRequest = {}) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.size(8.dp))
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.size(16.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    items(tasks) { task ->
                        InstallingTaskItem(
                            title = task.title,
                            task = task.task,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.size(16.dp))

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onCancel
                ) {
                    MarqueeText(text = stringResource(R.string.generic_cancel))
                }
            }
        }
    }
}

@Composable
private fun InstallingTaskItem(
    title: String,
    task: Task,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val icon = when (task.taskState) {
            TaskState.PREPARING -> Icons.Outlined.Schedule
            TaskState.RUNNING -> Icons.Outlined.Download
            TaskState.COMPLETED -> Icons.Outlined.Check
        }
        Icon(
            modifier = Modifier.size(24.dp),
            imageVector = icon,
            contentDescription = null
        )

        Column(
            modifier = modifier
                .weight(1f)
                .animateContentSize(animationSpec = getAnimateTween())
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge
            )
            if (task.taskState == TaskState.RUNNING) {
                Spacer(modifier = Modifier.height(4.dp))
                task.currentMessageRes?.let { messageRes ->
                    val args = task.currentMessageArgs
                    Text(
                        text = if (args != null) {
                            stringResource(messageRes, *args)
                        } else {
                            stringResource(messageRes)
                        },
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                if (task.currentProgress < 0) { //负数则代表不确定
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        LinearProgressIndicator(
                            progress = { task.currentProgress },
                            modifier = Modifier
                                .weight(1f)
                                .align(Alignment.CenterVertically)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${(task.currentProgress * 100).toInt()}%",
                            modifier = Modifier.align(Alignment.CenterVertically),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}