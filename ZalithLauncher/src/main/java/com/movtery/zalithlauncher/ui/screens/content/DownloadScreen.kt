package com.movtery.zalithlauncher.ui.screens.content

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
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
import com.movtery.zalithlauncher.ui.screens.content.download.DownloadModPackScreen
import com.movtery.zalithlauncher.ui.screens.content.download.DownloadModScreen
import com.movtery.zalithlauncher.ui.screens.content.download.DownloadResourcePackScreen
import com.movtery.zalithlauncher.ui.screens.content.download.DownloadSavesScreen
import com.movtery.zalithlauncher.ui.screens.content.download.DownloadShadersScreen
import com.movtery.zalithlauncher.ui.screens.content.elements.CategoryIcon
import com.movtery.zalithlauncher.ui.screens.content.elements.CategoryItem
import com.movtery.zalithlauncher.ui.screens.navigateOnce
import com.movtery.zalithlauncher.ui.screens.navigateTo
import com.movtery.zalithlauncher.ui.screens.onBack
import com.movtery.zalithlauncher.utils.animation.swapAnimateDpAsState
import com.movtery.zalithlauncher.viewmodel.ErrorViewModel
import com.movtery.zalithlauncher.viewmodel.ScreenBackStackViewModel

/**
 * 导航至DownloadScreen
 */
fun ScreenBackStackViewModel.navigateToDownload(targetScreen: NavKey? = null) {
    downloadBackStack.clearWith(targetScreen ?: NestedNavKey.DownloadGame(downloadGameBackStack))
    mainScreenBackStack.navigateTo(
        screenKey = NestedNavKey.Download(backStack = downloadBackStack),
        useClassEquality = true
    )
}

@Composable
fun DownloadScreen(
    key: NestedNavKey.Download,
    backScreenViewModel: ScreenBackStackViewModel,
    summitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    BaseScreen(
        screenKey = key,
        currentKey = backScreenViewModel.mainScreenKey,
        useClassEquality = true
    ) { isVisible: Boolean ->
        Row(modifier = Modifier.fillMaxSize()) {
            TabMenu(
                modifier = Modifier.fillMaxHeight(),
                isVisible = isVisible,
                backStack = key.backStack,
                downloadScreenKey = backScreenViewModel.downloadScreenKey,
                gameBackStack = backScreenViewModel.downloadGameBackStack,
                modpackBackStack = backScreenViewModel.downloadModPackBackStack,
                modBackStack = backScreenViewModel.downloadModBackStack,
                resourcePackBackStack = backScreenViewModel.downloadResourcePackBackStack,
                savesBackStack = backScreenViewModel.downloadSavesBackStack,
                shadersBackStack = backScreenViewModel.downloadShadersBackStack
            )

            NavigationUI(
                key = key,
                backScreenViewModel = backScreenViewModel,
                summitError = summitError,
                modifier = Modifier.fillMaxHeight()
            )
        }
    }
}

@Composable
private fun TabMenu(
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    backStack: NavBackStack,
    downloadScreenKey: NavKey?,
    gameBackStack: NavBackStack,
    modpackBackStack: NavBackStack,
    modBackStack: NavBackStack,
    resourcePackBackStack: NavBackStack,
    savesBackStack: NavBackStack,
    shadersBackStack: NavBackStack
) {
    val downloadsList = listOf(
        CategoryItem(NestedNavKey.DownloadGame(gameBackStack), { CategoryIcon(Icons.Outlined.SportsEsports, R.string.download_category_game) }, R.string.download_category_game),
        CategoryItem(NestedNavKey.DownloadModPack(modpackBackStack), { CategoryIcon(R.drawable.ic_package_2, R.string.download_category_modpack) }, R.string.download_category_modpack),
        CategoryItem(NestedNavKey.DownloadMod(modBackStack), { CategoryIcon(Icons.Outlined.Extension, R.string.download_category_mod) }, R.string.download_category_mod, division = true),
        CategoryItem(NestedNavKey.DownloadResourcePack(resourcePackBackStack), { CategoryIcon(Icons.Outlined.Image, R.string.download_category_resource_pack) }, R.string.download_category_resource_pack),
        CategoryItem(NestedNavKey.DownloadSaves(savesBackStack), { CategoryIcon(Icons.Outlined.Public, R.string.download_category_saves) }, R.string.download_category_saves),
        CategoryItem(NestedNavKey.DownloadShaders(shadersBackStack), { CategoryIcon(Icons.Outlined.Lightbulb, R.string.download_category_shaders) }, R.string.download_category_shaders),
    )

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
                selected = downloadScreenKey == item.key,
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
    key: NestedNavKey.Download,
    backScreenViewModel: ScreenBackStackViewModel,
    summitError: (ErrorViewModel.ThrowableMessage) -> Unit,
    modifier: Modifier = Modifier
) {
    val backStack = key.backStack
    val stackTopKey = backStack.lastOrNull()
    LaunchedEffect(stackTopKey) {
        backScreenViewModel.downloadScreenKey = stackTopKey
    }

    if (backStack.isNotEmpty()) {
        NavDisplay(
            backStack = backStack,
            modifier = modifier,
            onBack = {
                onBack(backStack)
            },
            entryProvider = entryProvider {
                entry<NestedNavKey.DownloadGame> { key ->
                    DownloadGameScreen(
                        key = key,
                        mainScreenKey = backScreenViewModel.mainScreenKey,
                        downloadScreenKey = backScreenViewModel.downloadScreenKey,
                        downloadGameScreenKey = backScreenViewModel.downloadGameScreenKey,
                        onCurrentKeyChange = { newKey ->
                            backScreenViewModel.downloadGameScreenKey = newKey
                        }
                    )
                }
                entry<NestedNavKey.DownloadModPack> { key ->
                    DownloadModPackScreen(
                        key = key,
                        mainScreenKey = backScreenViewModel.mainScreenKey,
                        downloadScreenKey = backScreenViewModel.downloadScreenKey,
                        downloadModPackScreenKey = backScreenViewModel.downloadModPackScreenKey,
                        onCurrentKeyChange = { newKey ->
                            backScreenViewModel.downloadModPackScreenKey = newKey
                        }
                    )
                }
                entry<NestedNavKey.DownloadMod> { key ->
                    DownloadModScreen(
                        key = key,
                        mainScreenKey = backScreenViewModel.mainScreenKey,
                        downloadScreenKey = backScreenViewModel.downloadScreenKey,
                        downloadModScreenKey = backScreenViewModel.downloadModScreenKey,
                        onCurrentKeyChange = { newKey ->
                            backScreenViewModel.downloadModScreenKey = newKey
                        },
                        summitError = summitError
                    )
                }
                entry<NestedNavKey.DownloadResourcePack> { key ->
                    DownloadResourcePackScreen(
                        key = key,
                        mainScreenKey = backScreenViewModel.mainScreenKey,
                        downloadScreenKey = backScreenViewModel.downloadScreenKey,
                        downloadResourcePackScreenKey = backScreenViewModel.downloadResourcePackScreenKey,
                        onCurrentKeyChange = { newKey ->
                            backScreenViewModel.downloadResourcePackScreenKey = newKey
                        },
                        summitError = summitError
                    )
                }
                entry<NestedNavKey.DownloadSaves> { key ->
                    DownloadSavesScreen(
                        key = key,
                        mainScreenKey = backScreenViewModel.mainScreenKey,
                        downloadScreenKey = backScreenViewModel.downloadScreenKey,
                        downloadSavesScreenKey = backScreenViewModel.downloadSavesScreenKey,
                        onCurrentKeyChange = { newKey ->
                            backScreenViewModel.downloadSavesScreenKey = newKey
                        },
                        summitError = summitError
                    )
                }
                entry<NestedNavKey.DownloadShaders> { key ->
                    DownloadShadersScreen(
                        key = key,
                        mainScreenKey = backScreenViewModel.mainScreenKey,
                        downloadScreenKey = backScreenViewModel.downloadScreenKey,
                        downloadShadersScreenKey = backScreenViewModel.downloadShadersScreenKey,
                        onCurrentKeyChange = { newKey ->
                            backScreenViewModel.downloadShadersScreenKey = newKey
                        },
                        summitError = summitError
                    )
                }
            }
        )
    } else {
        Box(modifier)
    }
}