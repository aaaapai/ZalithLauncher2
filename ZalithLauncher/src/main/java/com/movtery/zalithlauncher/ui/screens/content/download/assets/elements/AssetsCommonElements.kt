package com.movtery.zalithlauncher.ui.screens.content.download.assets.elements

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.download.assets.platform.Platform
import com.movtery.zalithlauncher.ui.components.ShimmerBox
import com.movtery.zalithlauncher.utils.logging.Logger.lWarning

/**
 * 平台标识元素，展示平台Logo + 平台名称
 */
@Composable
fun PlatformIdentifier(
    modifier: Modifier = Modifier,
    platform: Platform,
    iconSize: Dp = 12.dp,
    color: Color = MaterialTheme.colorScheme.tertiary,
    contentColor: Color = MaterialTheme.colorScheme.onTertiary,
    shape: Shape = MaterialTheme.shapes.large,
    textStyle: TextStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
) {
    Surface(
        modifier = modifier,
        color = color,
        contentColor = contentColor,
        shape = shape
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                modifier = Modifier.size(iconSize),
                painter = painterResource(platform.getDrawable()),
                contentDescription = platform.displayName
            )
            Text(
                text = platform.displayName,
                style = textStyle
            )
        }
    }
}

/**
 * 获取平台的LOGO
 */
fun Platform.getDrawable() = when (this) {
    Platform.CURSEFORGE -> R.drawable.img_platform_curseforge
    Platform.MODRINTH -> R.drawable.img_platform_modrinth
}

/**
 * 资源封面网络图标
 * @param iconUrl 图标链接
 */
@Composable
fun AssetsIcon(
    modifier: Modifier = Modifier,
    iconUrl: String? = null,
    colorFilter: ColorFilter? = null
) {
    val context = LocalContext.current

    val imageRequest = remember(iconUrl) {
        iconUrl?.takeIf { it.isNotBlank() }?.let {
            ImageRequest.Builder(context)
                .data(it)
                .listener(
                    onError = { _, result -> lWarning("Coil: error = ${result.throwable}") }
                )
                .crossfade(true)
                .build()
        }
    }

    val painter = rememberAsyncImagePainter(
        model = imageRequest,
        placeholder = null,
        error = painterResource(R.drawable.ic_unknown_icon)
    )
    val state by painter.state.collectAsState()

    when (state) {
        AsyncImagePainter.State.Empty -> {
            Box(modifier = modifier)
        }
        is AsyncImagePainter.State.Error, is AsyncImagePainter.State.Loading -> {
            ShimmerBox(
                modifier = modifier
            )
        }
        is AsyncImagePainter.State.Success -> {
            Image(
                painter = painter,
                contentDescription = null,
                alignment = Alignment.Center,
                contentScale = ContentScale.Fit,
                modifier = modifier,
                colorFilter = colorFilter
            )
        }
    }
}