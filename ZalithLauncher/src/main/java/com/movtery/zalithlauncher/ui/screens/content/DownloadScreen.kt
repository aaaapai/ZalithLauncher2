package com.movtery.zalithlauncher.ui.screens.content

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.state.MutableStates
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.secondaryContainerDrawerItemColors
import com.movtery.zalithlauncher.ui.screens.content.download.DOWNLOAD_GAME_SCREEN_TAG
import com.movtery.zalithlauncher.ui.screens.content.download.DOWNLOAD_MOD_PACK_SCREEN_TAG
import com.movtery.zalithlauncher.ui.screens.content.download.DOWNLOAD_MOD_SCREEN_TAG
import com.movtery.zalithlauncher.ui.screens.content.download.DOWNLOAD_RESOURCE_PACK_TAG
import com.movtery.zalithlauncher.ui.screens.content.download.DOWNLOAD_SAVES_SCREEN_TAG
import com.movtery.zalithlauncher.ui.screens.content.download.DOWNLOAD_SHADERS_SCREEN_TAG
import com.movtery.zalithlauncher.ui.screens.content.download.DownloadGameScreen
import com.movtery.zalithlauncher.ui.screens.content.download.DownloadModPackScreen
import com.movtery.zalithlauncher.ui.screens.content.download.DownloadModScreen
import com.movtery.zalithlauncher.ui.screens.content.download.DownloadResourcePackScreen
import com.movtery.zalithlauncher.ui.screens.content.download.DownloadSavesScreen
import com.movtery.zalithlauncher.ui.screens.content.download.DownloadShadersScreen
import com.movtery.zalithlauncher.ui.screens.content.elements.CategoryIcon
import com.movtery.zalithlauncher.ui.screens.navigateOnce
import com.movtery.zalithlauncher.utils.animation.TransitionAnimationType
import com.movtery.zalithlauncher.utils.animation.getAnimateTween
import com.movtery.zalithlauncher.utils.animation.getAnimateType
import com.movtery.zalithlauncher.utils.animation.swapAnimateDpAsState

const val DOWNLOAD_SCREEN_TAG = "DownloadScreen"

@Composable
fun DownloadScreen(
    startDestination: String? = null
) {
    BaseScreen(
        screenTag = DOWNLOAD_SCREEN_TAG,
        currentTag = MutableStates.mainScreenTag,
        tagStartWith = true
    ) { isVisible: Boolean ->
        val navController = rememberNavController()

        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            TabMenu(
                isVisible = isVisible,
                navController = navController,
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(2.2f)
            )

            NavigationUI(
                startDestination = startDestination ?: DOWNLOAD_GAME_SCREEN_TAG,
                navController = navController,
                modifier = Modifier.weight(7.8f)
            )
        }
    }
}

private data class DownloadsItem(
    val screenTag: String,
    val icon: @Composable () -> Unit,
    val textRes: Int,
    val division: Boolean = false
)

private val downloadsList = listOf(
    DownloadsItem(DOWNLOAD_GAME_SCREEN_TAG, { CategoryIcon(Icons.Outlined.SportsEsports, R.string.download_category_game) }, R.string.download_category_game),
    DownloadsItem(DOWNLOAD_MOD_PACK_SCREEN_TAG, { CategoryIcon(R.drawable.ic_package_2, R.string.download_category_modpack) }, R.string.download_category_modpack),
    DownloadsItem(DOWNLOAD_MOD_SCREEN_TAG, { CategoryIcon(Icons.Outlined.Extension, R.string.download_category_mod) }, R.string.download_category_mod, division = true),
    DownloadsItem(DOWNLOAD_RESOURCE_PACK_TAG, { CategoryIcon(Icons.Outlined.Image, R.string.download_category_resource_pack) }, R.string.download_category_resource_pack),
    DownloadsItem(DOWNLOAD_SAVES_SCREEN_TAG, { CategoryIcon(Icons.Outlined.Public, R.string.download_category_saves) }, R.string.download_category_saves),
    DownloadsItem(DOWNLOAD_SHADERS_SCREEN_TAG, { CategoryIcon(Icons.Outlined.Lightbulb, R.string.download_category_shaders) }, R.string.download_category_shaders),
)

@Composable
private fun TabMenu(
    isVisible: Boolean,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val xOffset by swapAnimateDpAsState(
        targetValue = (-40).dp,
        swapIn = isVisible
    )

    LazyColumn(
        modifier = modifier
            .offset { IntOffset(x = xOffset.roundToPx(), y = 0) }
            .padding(start = 12.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        items(downloadsList.size) { index ->
            val item = downloadsList[index]
            if (item.division) {
                HorizontalDivider(
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 12.dp)
                        .fillMaxWidth()
                        .alpha(0.5f),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            NavigationDrawerItem(
                icon = {
                    item.icon()
                },
                label = {
                    Text(
                        text = stringResource(item.textRes),
                        softWrap = true,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                selected = MutableStates.downloadScreenTag == item.screenTag,
                onClick = {
                    navController.navigateOnce(item.screenTag)
                },
                colors = secondaryContainerDrawerItemColors()
            )
        }
    }
}

@Composable
private fun NavigationUI(
    startDestination: String,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            MutableStates.downloadScreenTag = destination.route
        }
        navController.addOnDestinationChangedListener(listener)
    }

    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            if (getAnimateType() != TransitionAnimationType.CLOSE) {
                fadeIn(animationSpec = getAnimateTween())
            } else {
                EnterTransition.None
            }
        },
        exitTransition = {
            if (getAnimateType() != TransitionAnimationType.CLOSE) {
                fadeOut(animationSpec = getAnimateTween())
            } else {
                ExitTransition.None
            }
        }
    ) {
        composable(
            route = DOWNLOAD_GAME_SCREEN_TAG
        ) {
            DownloadGameScreen()
        }
        composable(
            route = DOWNLOAD_MOD_PACK_SCREEN_TAG
        ) {
            DownloadModPackScreen()
        }
        composable(
            route = DOWNLOAD_MOD_SCREEN_TAG
        ) {
            DownloadModScreen()
        }
        composable(
            route = DOWNLOAD_RESOURCE_PACK_TAG
        ) {
            DownloadResourcePackScreen()
        }
        composable(
            route = DOWNLOAD_SAVES_SCREEN_TAG
        ) {
            DownloadSavesScreen()
        }
        composable(
            route = DOWNLOAD_SHADERS_SCREEN_TAG
        ) {
            DownloadShadersScreen()
        }
    }
}