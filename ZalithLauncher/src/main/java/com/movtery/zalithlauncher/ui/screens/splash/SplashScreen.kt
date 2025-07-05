package com.movtery.zalithlauncher.ui.screens.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.movtery.zalithlauncher.components.InstallableItem
import com.movtery.zalithlauncher.info.InfoDistributor
import com.movtery.zalithlauncher.ui.screens.splash.elements.splashScreenKey

/**
 * @param startAllTask 开启全部的解压任务
 * @param unpackItems 解压任务列表
 */
@Composable
fun SplashScreen(
    startAllTask: () -> Unit,
    unpackItems: List<InstallableItem>,
) {
    val backStack = rememberNavBackStack(UnpackScreenKey)

    Column {
        TopBar(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .zIndex(10f),
            color = MaterialTheme.colorScheme.surfaceContainer
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            NavigationUI(
                startAllTask = startAllTask,
                backStack = backStack,
                unpackItems = unpackItems,
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = MaterialTheme.colorScheme.surface)
            )
        }
    }
}

@Composable
private fun TopBar(
    modifier: Modifier = Modifier,
    color: Color
) {
    Surface(
        modifier = modifier,
        color = color,
        tonalElevation = 3.dp
    ) {
        Row(
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                modifier = Modifier.align(Alignment.CenterVertically),
                text = InfoDistributor.LAUNCHER_NAME
            )
        }
    }
}

@Composable
private fun NavigationUI(
    modifier: Modifier = Modifier,
    backStack: NavBackStack,
    startAllTask: () -> Unit,
    unpackItems: List<InstallableItem>,
) {
    val currentKey = backStack.lastOrNull()
    LaunchedEffect(currentKey) {
        splashScreenKey = currentKey
    }

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        entryProvider = entryProvider {
            entry<UnpackScreenKey> {
                UnpackScreen(unpackItems) {
                    startAllTask()
                }
            }
        }
    )
}