package com.movtery.zalithlauncher.ui.screens.splash

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.components.InstallableItem
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.MarqueeText
import com.movtery.zalithlauncher.ui.components.ScalingActionButton
import com.movtery.zalithlauncher.ui.components.itemLayoutColor
import com.movtery.zalithlauncher.ui.screens.splash.elements.splashScreenKey
import com.movtery.zalithlauncher.utils.animation.getAnimateTween
import com.movtery.zalithlauncher.utils.animation.swapAnimateDpAsState
import kotlinx.serialization.Serializable

@Serializable
data object UnpackScreenKey: NavKey

@Composable
fun UnpackScreen(
    items: List<InstallableItem>,
    onAgreeClick: () -> Unit = {}
) {
    BaseScreen(
        screenKey = UnpackScreenKey,
        currentKey = splashScreenKey
    ) { isVisible ->
        Row(modifier = Modifier.fillMaxSize()) {
            UnpackTaskList(
                isVisible = isVisible,
                items = items,
                modifier = Modifier
                    .weight(7f)
                    .fillMaxHeight()
                    .padding(start = 12.dp, top = 12.dp, bottom = 12.dp)
            )

            ActionMenu(
                isVisible = isVisible,
                modifier = Modifier
                    .weight(3f)
                    .fillMaxHeight()
                    .padding(all = 12.dp),
                onAgreeClick = onAgreeClick
            )
        }
    }
}

@Composable
private fun ActionMenu(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    onAgreeClick: () -> Unit = {}
) {
    var installing by remember { mutableStateOf(false) }

    val xOffset by swapAnimateDpAsState(
        targetValue = 40.dp,
        swapIn = isVisible,
        isHorizontal = true
    )

    Card(
        modifier = modifier
            .offset {
                IntOffset(
                    x = xOffset.roundToPx(),
                    y = 0
                )
            },
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column {
            Column(
                modifier = Modifier
                    .verticalScroll(state = rememberScrollState())
                    .weight(1f)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    text = if (installing) {
                        stringResource(R.string.splash_screen_installing)
                    } else {
                        stringResource(R.string.splash_screen_unpack_desc)
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            ScalingActionButton(
                enabled = !installing,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 8.dp),
                onClick = {
                    installing = true
                    onAgreeClick()
                }
            ) {
                MarqueeText(text = stringResource(R.string.splash_screen_agree))
            }
        }
    }
}

@Composable
private fun UnpackTaskList(
    isVisible: Boolean,
    items: List<InstallableItem>,
    modifier: Modifier = Modifier,
) {
    val yOffset by swapAnimateDpAsState(
        targetValue = (-40).dp,
        swapIn = isVisible
    )

    Card(
        modifier = modifier
            .offset {
                IntOffset(
                    x = 0,
                    y = yOffset.roundToPx()
                )
            },
        shape = MaterialTheme.shapes.extraLarge
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
            items(items) { item ->
                TaskItem(
                    item = item,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun TaskItem(
    item: InstallableItem,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = itemLayoutColor(),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 1.dp
    ) {
        Row {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, top = 8.dp, bottom = 8.dp)
                    .animateContentSize(animationSpec = getAnimateTween())
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.labelMedium
                )
                item.summary?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                if (item.isRunning) {
                    item.task.taskMessage?.let { taskMessage ->
                        Text(
                            text = taskMessage,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    }
                }
            }

            val iconModifier = Modifier
                .align(Alignment.CenterVertically)
                .padding(PaddingValues(horizontal = 12.dp, vertical = 8.dp))
                .size(18.dp)
            if (item.isRunning) {
                CircularProgressIndicator(
                    modifier = iconModifier,
                    strokeWidth = 2.dp
                )
            } else if (item.isFinished) {
                Icon(
                    modifier = iconModifier,
                    imageVector = Icons.Default.Done,
                    contentDescription = null
                )
            }
        }
    }
}