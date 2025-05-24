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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.VideoSettings
import androidx.compose.material.icons.outlined.VideogameAsset
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
import com.movtery.zalithlauncher.ui.screens.content.elements.CategoryIcon
import com.movtery.zalithlauncher.ui.screens.content.elements.CategoryItem
import com.movtery.zalithlauncher.ui.screens.content.settings.ABOUT_INFO_SCREEN_TAG
import com.movtery.zalithlauncher.ui.screens.content.settings.AboutInfoScreen
import com.movtery.zalithlauncher.ui.screens.content.settings.CONTROL_MANAGE_SCREEN_TAG
import com.movtery.zalithlauncher.ui.screens.content.settings.CONTROL_SETTINGS_SCREEN_TAG
import com.movtery.zalithlauncher.ui.screens.content.settings.ControlManageScreen
import com.movtery.zalithlauncher.ui.screens.content.settings.ControlSettingsScreen
import com.movtery.zalithlauncher.ui.screens.content.settings.GAME_SETTINGS_TAG
import com.movtery.zalithlauncher.ui.screens.content.settings.GameSettingsScreen
import com.movtery.zalithlauncher.ui.screens.content.settings.JAVA_MANAGE_SCREEN_TAG
import com.movtery.zalithlauncher.ui.screens.content.settings.JavaManageScreen
import com.movtery.zalithlauncher.ui.screens.content.settings.LAUNCHER_SETTINGS_TAG
import com.movtery.zalithlauncher.ui.screens.content.settings.LauncherSettingsScreen
import com.movtery.zalithlauncher.ui.screens.content.settings.RENDERER_SETTINGS_SCREEN_TAG
import com.movtery.zalithlauncher.ui.screens.content.settings.RendererSettingsScreen
import com.movtery.zalithlauncher.ui.screens.navigateOnce
import com.movtery.zalithlauncher.utils.animation.TransitionAnimationType
import com.movtery.zalithlauncher.utils.animation.getAnimateTween
import com.movtery.zalithlauncher.utils.animation.getAnimateType
import com.movtery.zalithlauncher.utils.animation.swapAnimateDpAsState

const val SETTINGS_SCREEN_TAG = "SettingsScreen"

@Composable
fun SettingsScreen() {
    BaseScreen(
        screenTag = SETTINGS_SCREEN_TAG,
        currentTag = MutableStates.mainScreenTag
    ) { isVisible ->
        val settingsNavController = rememberNavController()

        Row(modifier = Modifier.fillMaxSize()) {
            TabMenu(
                isVisible = isVisible,
                settingsNavController = settingsNavController,
                modifier = Modifier.fillMaxHeight()
            )
            NavigationUI(
                settingsNavController = settingsNavController,
                modifier = Modifier.fillMaxHeight()
            )
        }
    }
}

private val settingItems = listOf(
    CategoryItem(RENDERER_SETTINGS_SCREEN_TAG, { CategoryIcon(Icons.Outlined.VideoSettings, R.string.settings_tab_renderer) }, R.string.settings_tab_renderer),
    CategoryItem(GAME_SETTINGS_TAG, { CategoryIcon(Icons.Outlined.RocketLaunch, R.string.settings_tab_game) }, R.string.settings_tab_game),
    CategoryItem(CONTROL_SETTINGS_SCREEN_TAG, { CategoryIcon(Icons.Outlined.VideogameAsset, R.string.settings_tab_control) }, R.string.settings_tab_control),
    CategoryItem(LAUNCHER_SETTINGS_TAG, { CategoryIcon(R.drawable.ic_setting_launcher, R.string.settings_tab_launcher) }, R.string.settings_tab_launcher),
    CategoryItem(JAVA_MANAGE_SCREEN_TAG, { CategoryIcon(R.drawable.ic_java, R.string.settings_tab_java_manage) }, R.string.settings_tab_java_manage, division = true),
    CategoryItem(CONTROL_MANAGE_SCREEN_TAG, { CategoryIcon(Icons.Outlined.VideogameAsset, R.string.settings_tab_control_manage) }, R.string.settings_tab_control_manage),
    CategoryItem(ABOUT_INFO_SCREEN_TAG, { CategoryIcon(Icons.Outlined.Info, R.string.settings_tab_info_about) }, R.string.settings_tab_info_about, division = true)
)

@Composable
private fun TabMenu(
    isVisible: Boolean,
    settingsNavController: NavController,
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
        settingItems.forEach { item ->
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
                selected = MutableStates.settingsScreenTag == item.tag,
                onClick = {
                    settingsNavController.navigateOnce(item.tag)
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
    settingsNavController: NavHostController,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(settingsNavController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            MutableStates.settingsScreenTag = destination.route
        }
        settingsNavController.addOnDestinationChangedListener(listener)
    }

    NavHost(
        modifier = modifier,
        navController = settingsNavController,
        startDestination = RENDERER_SETTINGS_SCREEN_TAG,
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
            route = RENDERER_SETTINGS_SCREEN_TAG
        ) {
            RendererSettingsScreen()
        }
        composable(
            route = GAME_SETTINGS_TAG
        ) {
            GameSettingsScreen()
        }
        composable(
            route = CONTROL_SETTINGS_SCREEN_TAG
        ) {
            ControlSettingsScreen()
        }
        composable(
            route = LAUNCHER_SETTINGS_TAG
        ) {
            LauncherSettingsScreen()
        }
        composable(
            route = JAVA_MANAGE_SCREEN_TAG
        ) {
            JavaManageScreen()
        }
        composable(
            route = CONTROL_MANAGE_SCREEN_TAG
        ) {
            ControlManageScreen()
        }
        composable(
            route = ABOUT_INFO_SCREEN_TAG
        ) {
            AboutInfoScreen()
        }
    }
}