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
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Public
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
import com.movtery.zalithlauncher.ui.screens.content.versions.SAVES_MANAGER_SCREEN_TAG
import com.movtery.zalithlauncher.ui.screens.content.versions.SavesManagerScreen
import com.movtery.zalithlauncher.ui.screens.content.versions.VERSION_CONFIG_SCREEN_TAG
import com.movtery.zalithlauncher.ui.screens.content.versions.VERSION_OVERVIEW_SCREEN_TAG
import com.movtery.zalithlauncher.ui.screens.content.versions.VersionConfigScreen
import com.movtery.zalithlauncher.ui.screens.content.versions.VersionOverViewScreen
import com.movtery.zalithlauncher.ui.screens.navigateOnce
import com.movtery.zalithlauncher.utils.animation.TransitionAnimationType
import com.movtery.zalithlauncher.utils.animation.getAnimateTween
import com.movtery.zalithlauncher.utils.animation.getAnimateType
import com.movtery.zalithlauncher.utils.animation.swapAnimateDpAsState

const val VERSION_SETTINGS_SCREEN_TAG = "VersionSettingsScreen"

@Composable
fun VersionSettingsScreen() {
    BaseScreen(
        screenTag = VERSION_SETTINGS_SCREEN_TAG,
        currentTag = MutableStates.mainScreenTag
    ) { isVisible ->
        val navController = rememberNavController()

        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            TabMenu(
                isVisible = isVisible,
                navController = navController,
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(2.5f)
            )

            NavigationUI(
                navController = navController,
                modifier = Modifier.weight(7.5f)
            )
        }
    }
}

private val settingItems = listOf(
    VersionSettingsItem(VERSION_OVERVIEW_SCREEN_TAG, { CategoryIcon(Icons.Outlined.Dashboard, R.string.versions_settings_overview) }, R.string.versions_settings_overview),
    VersionSettingsItem(VERSION_CONFIG_SCREEN_TAG, { CategoryIcon(Icons.Outlined.Build, R.string.versions_settings_config) }, R.string.versions_settings_config),
    VersionSettingsItem(SAVES_MANAGER_SCREEN_TAG, { CategoryIcon(Icons.Outlined.Public, R.string.saves_manage) }, R.string.saves_manage, division = true)
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
                selected = MutableStates.versionSettingsScreenTag == item.screenTag,
                onClick = {
                    navController.navigateOnce(item.screenTag)
                },
                colors = secondaryContainerDrawerItemColors()
            )
        }
    }
}

private data class VersionSettingsItem(
    val screenTag: String,
    val icon: @Composable () -> Unit,
    val textRes: Int,
    val division: Boolean = false
)

@Composable
private fun NavigationUI(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            MutableStates.versionSettingsScreenTag = destination.route
        }
        navController.addOnDestinationChangedListener(listener)
    }

    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = VERSION_OVERVIEW_SCREEN_TAG,
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
            route = VERSION_OVERVIEW_SCREEN_TAG
        ) {
            VersionOverViewScreen()
        }
        composable(
            route = VERSION_CONFIG_SCREEN_TAG
        ) {
            VersionConfigScreen()
        }
        composable(
            route = SAVES_MANAGER_SCREEN_TAG
        ) {
            SavesManagerScreen()
        }
    }
}