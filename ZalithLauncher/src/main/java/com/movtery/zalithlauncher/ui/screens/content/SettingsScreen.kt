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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.VideoSettings
import androidx.compose.material.icons.outlined.VideogameAsset
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
import com.movtery.zalithlauncher.ui.screens.content.elements.CategoryIcon
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

        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            TabMenu(
                isVisible = isVisible,
                settingsNavController = settingsNavController,
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(2.5f)
            )

            NavigationUI(
                settingsNavController = settingsNavController,
                modifier = Modifier.weight(7.5f)
            )
        }
    }
}

private val settingItems = listOf(
    SettingsItem(RENDERER_SETTINGS_SCREEN_TAG, { CategoryIcon(Icons.Outlined.VideoSettings, R.string.settings_tab_renderer) }, R.string.settings_tab_renderer),
    SettingsItem(GAME_SETTINGS_TAG, { CategoryIcon(Icons.Outlined.RocketLaunch, R.string.settings_tab_game) }, R.string.settings_tab_game),
    SettingsItem(CONTROL_SETTINGS_SCREEN_TAG, { CategoryIcon(Icons.Outlined.VideogameAsset, R.string.settings_tab_control) }, R.string.settings_tab_control),
    SettingsItem(LAUNCHER_SETTINGS_TAG, { CategoryIcon(R.drawable.ic_setting_launcher, R.string.settings_tab_launcher) }, R.string.settings_tab_launcher),
    SettingsItem(JAVA_MANAGE_SCREEN_TAG, { CategoryIcon(R.drawable.ic_java, R.string.settings_tab_java_manage) }, R.string.settings_tab_java_manage, division = true),
    SettingsItem(CONTROL_MANAGE_SCREEN_TAG, { CategoryIcon(Icons.Outlined.VideogameAsset, R.string.settings_tab_control_manage) }, R.string.settings_tab_control_manage),
    SettingsItem(ABOUT_INFO_SCREEN_TAG, { CategoryIcon(Icons.Outlined.Info, R.string.settings_tab_info_about) }, R.string.settings_tab_info_about, division = true)
)

@Composable
private fun TabMenu(
    isVisible: Boolean,
    settingsNavController: NavController,
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
        items(settingItems) { item ->
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
                selected = MutableStates.settingsScreenTag == item.screenTag,
                onClick = {
                    settingsNavController.navigateOnce(item.screenTag)
                },
                colors = secondaryContainerDrawerItemColors()
            )
        }
    }
}

private data class SettingsItem(
    val screenTag: String,
    val icon: @Composable () -> Unit,
    val textRes: Int,
    val division: Boolean = false
)

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