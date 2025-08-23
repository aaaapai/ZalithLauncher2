package com.movtery.zalithlauncher.ui.screens.game.elements

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.bridge.ZLBridgeStates
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun DraggableGameBall(
    alignment: Alignment = Alignment.TopCenter,
    showGameFps: Boolean,
    onClick: () -> Unit = {}
) {
    var offset by remember { mutableStateOf(Offset.Zero) }
    var ballSize by remember { mutableStateOf(IntSize(0, 0)) }
    var initialized by remember { mutableStateOf(false) }

    val containerSize = LocalWindowInfo.current.containerSize
    val screenWidthPx = containerSize.width.toFloat()
    val screenHeightPx = containerSize.height.toFloat()
    val viewConfig = LocalViewConfiguration.current
    val layoutDirection = LocalLayoutDirection.current

    val offset1 by rememberUpdatedState(offset)
    val ballSize1 by rememberUpdatedState(ballSize)
    val screenWidthPx1 by rememberUpdatedState(screenWidthPx)
    val screenHeightPx1 by rememberUpdatedState(screenHeightPx)

    GameBall(
        modifier = Modifier
            .onGloballyPositioned { layoutCoordinates ->
                ballSize = layoutCoordinates.size
                //初始化后，放置悬浮球到预定位置
                if (!initialized && ballSize.width > 0 && ballSize.height > 0) {
                    val placeableSize = IntSize(
                        screenWidthPx.toInt() - ballSize.width,
                        screenHeightPx.toInt() - ballSize.height
                    )
                    val alignedOffset = alignment.align(IntSize.Zero, placeableSize, layoutDirection)
                    offset = Offset(alignedOffset.x.toFloat(), alignedOffset.y.toFloat())
                    initialized = true
                }
            }
            .offset {
                IntOffset(offset.x.roundToInt(), offset.y.roundToInt())
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)

                    val startPosition = down.position
                    var isDragging = false

                    drag(down.id) { change ->
                        val delta = change.positionChange()
                        val distanceFromStart = (change.position - startPosition).getDistance()

                        if (!isDragging && distanceFromStart > viewConfig.touchSlop) {
                            //超出了拖动检测距离，说明是真的在进行拖动
                            //标记当前为拖动，避免松开手指后，判定为点击事件
                            isDragging = true
                        }

                        if (isDragging) { //只有在拖动的情况下，才会变更悬浮球的位置
                            val newX = offset1.x + delta.x
                            val newY = offset1.y + delta.y
                            val maxX = screenWidthPx1 - ballSize1.width
                            val maxY = screenHeightPx1 - ballSize1.height
                            offset = Offset(
                                x = max(0f, min(newX, maxX)),
                                y = max(0f, min(newY, maxY))
                            )
                        }
                        change.consume()
                    }

                    if (!isDragging) {
                        //非拖动事件，判定为一次点击
                        onClick()
                    }
                }
            },
        showGameFps = showGameFps
    )
}

@Composable
private fun GameBall(
    modifier: Modifier = Modifier,
    showGameFps: Boolean
) {
    Surface(
        modifier = modifier.wrapContentSize(),
        color = Color.Black.copy(alpha = 0.25f),
        contentColor = Color.White.copy(alpha = 0.95f),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .padding(all = 2.dp)
                .animateContentSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.size(28.dp),
                imageVector = Icons.Default.Settings,
                contentDescription = null
            )
            if (showGameFps) {
                Row(modifier = Modifier.padding(horizontal = 4.dp)) {
                    val fps = ZLBridgeStates.currentFPS
                    Text(
                        text = "FPS: $fps",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}