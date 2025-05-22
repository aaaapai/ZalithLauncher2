package com.movtery.zalithlauncher.ui.screens.content

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
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
import com.movtery.zalithlauncher.ui.screens.content.elements.CategoryItem
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

        Row(modifier = Modifier.fillMaxSize()) {
            TabMenu(
                isVisible = isVisible,
                navController = navController,
                modifier = Modifier.fillMaxHeight()
            )

            NavigationUI(
                startDestination = startDestination ?: DOWNLOAD_GAME_SCREEN_TAG,
                navController = navController,
                modifier = Modifier.fillMaxHeight()
            )
        }
    }
}

private val downloadsList = listOf(
    CategoryItem(DOWNLOAD_GAME_SCREEN_TAG, { CategoryIcon(Icons.Outlined.SportsEsports, R.string.download_category_game) }, R.string.download_category_game),
    CategoryItem(DOWNLOAD_MOD_PACK_SCREEN_TAG, { CategoryIcon(R.drawable.ic_package_2, R.string.download_category_modpack) }, R.string.download_category_modpack),
    CategoryItem(DOWNLOAD_MOD_SCREEN_TAG, { CategoryIcon(Icons.Outlined.Extension, R.string.download_category_mod) }, R.string.download_category_mod, division = true),
    CategoryItem(DOWNLOAD_RESOURCE_PACK_TAG, { CategoryIcon(Icons.Outlined.Image, R.string.download_category_resource_pack) }, R.string.download_category_resource_pack),
    CategoryItem(DOWNLOAD_SAVES_SCREEN_TAG, { CategoryIcon(Icons.Outlined.Public, R.string.download_category_saves) }, R.string.download_category_saves),
    CategoryItem(DOWNLOAD_SHADERS_SCREEN_TAG, { CategoryIcon(Icons.Outlined.Lightbulb, R.string.download_category_shaders) }, R.string.download_category_shaders),
)

@Composable
private fun TabMenu(
    isVisible: Boolean,
    navController: NavController,
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
                selected = MutableStates.downloadScreenTag == item.tag,
                onClick = {
                    navController.navigateOnce(item.tag)
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