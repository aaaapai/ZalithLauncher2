package com.movtery.zalithlauncher.ui.screens.content

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.clearWith
import com.movtery.zalithlauncher.ui.screens.content.download.DownloadGameScreen
import com.movtery.zalithlauncher.ui.screens.content.download.DownloadGameScreenKey
import com.movtery.zalithlauncher.ui.screens.content.download.DownloadModPackScreen
import com.movtery.zalithlauncher.ui.screens.content.download.DownloadModPackScreenKey
import com.movtery.zalithlauncher.ui.screens.content.download.DownloadModScreen
import com.movtery.zalithlauncher.ui.screens.content.download.DownloadModScreenKey
import com.movtery.zalithlauncher.ui.screens.content.download.DownloadResourcePackScreen
import com.movtery.zalithlauncher.ui.screens.content.download.DownloadResourcePackScreenKey
import com.movtery.zalithlauncher.ui.screens.content.download.DownloadSavesScreen
import com.movtery.zalithlauncher.ui.screens.content.download.DownloadSavesScreenKey
import com.movtery.zalithlauncher.ui.screens.content.download.DownloadShadersScreen
import com.movtery.zalithlauncher.ui.screens.content.download.DownloadShadersScreenKey
import com.movtery.zalithlauncher.ui.screens.content.elements.CategoryIcon
import com.movtery.zalithlauncher.ui.screens.content.elements.CategoryItem
import com.movtery.zalithlauncher.ui.screens.main.elements.mainScreenKey
import com.movtery.zalithlauncher.ui.screens.navigateOnce
import com.movtery.zalithlauncher.ui.screens.navigateTo
import com.movtery.zalithlauncher.utils.animation.swapAnimateDpAsState
import kotlinx.serialization.Serializable

@Serializable
data class DownloadScreenKey(
    val startKey: NavKey? = null
): NestedNavKey {
    override fun isLastScreen(): Boolean = downloadScreenBackStack.size <= 1
}

/**
 * 下载屏幕堆栈
 */
val downloadScreenBackStack = mutableStateListOf<NavKey>()

/**
 * 状态：下载屏幕的标签
 */
var downloadScreenKey by mutableStateOf<NavKey?>(null)

/**
 * 导航至DownloadScreen
 */
fun NavBackStack.navigateToDownload(targetScreen: NavKey? = null) {
    this.navigateTo(DownloadScreenKey(targetScreen), true)
}

@Composable
fun DownloadScreen(key: DownloadScreenKey) {
    downloadScreenBackStack.clearWith(key.startKey ?: DownloadGameScreenKey)

    BaseScreen(
        screenKey = key,
        currentKey = mainScreenKey,
        useClassEquality = true
    ) { isVisible: Boolean ->

        Row(modifier = Modifier.fillMaxSize()) {
            TabMenu(
                isVisible = isVisible,
                backStack = downloadScreenBackStack,
                modifier = Modifier.fillMaxHeight()
            )

            NavigationUI(
                backStack = downloadScreenBackStack,
                modifier = Modifier.fillMaxHeight()
            )
        }
    }
}

private val downloadsList = listOf(
    CategoryItem(DownloadGameScreenKey, { CategoryIcon(Icons.Outlined.SportsEsports, R.string.download_category_game) }, R.string.download_category_game),
    CategoryItem(DownloadModPackScreenKey, { CategoryIcon(R.drawable.ic_package_2, R.string.download_category_modpack) }, R.string.download_category_modpack),
    CategoryItem(DownloadModScreenKey, { CategoryIcon(Icons.Outlined.Extension, R.string.download_category_mod) }, R.string.download_category_mod, division = true),
    CategoryItem(DownloadResourcePackScreenKey, { CategoryIcon(Icons.Outlined.Image, R.string.download_category_resource_pack) }, R.string.download_category_resource_pack),
    CategoryItem(DownloadSavesScreenKey, { CategoryIcon(Icons.Outlined.Public, R.string.download_category_saves) }, R.string.download_category_saves),
    CategoryItem(DownloadShadersScreenKey, { CategoryIcon(Icons.Outlined.Lightbulb, R.string.download_category_shaders) }, R.string.download_category_shaders),
)

@Composable
private fun TabMenu(
    isVisible: Boolean,
    backStack: NavBackStack,
    modifier: Modifier = Modifier
) {
    val xOffset by swapAnimateDpAsState(
        targetValue = (-40).dp,
        swapIn = isVisible,
        isHorizontal = true
    )

    NavigationRail(
        modifier = modifier
            .width(IntrinsicSize.Min)
            .padding(start = 8.dp)
            .offset { IntOffset(x = xOffset.roundToPx(), y = 0) }
            .verticalScroll(rememberScrollState()),
        containerColor = Color.Transparent,
        windowInsets = WindowInsets(0)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        downloadsList.forEach { item ->
            if (item.division) {
                HorizontalDivider(
                    modifier = Modifier
                        .padding(all = 12.dp)
                        .fillMaxWidth()
                        .alpha(0.5f),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            NavigationRailItem(
                selected = downloadScreenKey === item.key,
                onClick = {
                    backStack.navigateOnce(item.key)
                },
                icon = {
                    item.icon()
                },
                label = {
                    Text(
                        modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                        text = stringResource(item.textRes),
                        overflow = TextOverflow.Clip,
                        maxLines = 1,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun NavigationUI(
    backStack: NavBackStack,
    modifier: Modifier = Modifier
) {
    val currentKey = backStack.lastOrNull()
    LaunchedEffect(currentKey) {
        downloadScreenKey = currentKey
    }

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = {
            val key = backStack.lastOrNull()
            if (key is NestedNavKey && !key.isLastScreen()) return@NavDisplay
            backStack.removeLastOrNull()
        },
        entryProvider = entryProvider {
            entry<DownloadGameScreenKey> { DownloadGameScreen() }
            entry<DownloadModPackScreenKey> { DownloadModPackScreen() }
            entry<DownloadModScreenKey> { DownloadModScreen() }
            entry<DownloadResourcePackScreenKey> { DownloadResourcePackScreen() }
            entry<DownloadSavesScreenKey> { DownloadSavesScreen() }
            entry<DownloadShadersScreenKey> { DownloadShadersScreen() }
        }
    )
}