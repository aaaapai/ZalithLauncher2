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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.rememberSceneSetupNavEntryDecorator
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.content.elements.CategoryIcon
import com.movtery.zalithlauncher.ui.screens.content.elements.CategoryItem
import com.movtery.zalithlauncher.ui.screens.content.settings.AboutInfoScreen
import com.movtery.zalithlauncher.ui.screens.content.settings.ControlManageScreen
import com.movtery.zalithlauncher.ui.screens.content.settings.ControlSettingsScreen
import com.movtery.zalithlauncher.ui.screens.content.settings.GameSettingsScreen
import com.movtery.zalithlauncher.ui.screens.content.settings.JavaManageScreen
import com.movtery.zalithlauncher.ui.screens.content.settings.LauncherSettingsScreen
import com.movtery.zalithlauncher.ui.screens.content.settings.RendererSettingsScreen
import com.movtery.zalithlauncher.ui.screens.navigateOnce
import com.movtery.zalithlauncher.ui.screens.onBack
import com.movtery.zalithlauncher.utils.animation.swapAnimateDpAsState
import com.movtery.zalithlauncher.viewmodel.ErrorViewModel
import com.movtery.zalithlauncher.viewmodel.ScreenBackStackViewModel

@Composable
fun SettingsScreen(
    key: NestedNavKey.Settings,
    backStackViewModel: ScreenBackStackViewModel,
    openLicenseScreen: (raw: Int) -> Unit,
    summitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    BaseScreen(
        screenKey = key,
        currentKey = backStackViewModel.mainScreenKey
    ) { isVisible ->

        Row(modifier = Modifier.fillMaxSize()) {
            TabMenu(
                modifier = Modifier.fillMaxHeight(),
                isVisible = isVisible,
                settingsScreenKey = backStackViewModel.settingsScreenKey,
                navigateTo = { settingKey ->
                    key.backStack.navigateOnce(settingKey)
                }
            )
            NavigationUI(
                key = key,
                mainScreenKey = backStackViewModel.mainScreenKey,
                settingsScreenKey = backStackViewModel.settingsScreenKey,
                onCurrentKeyChange = { newKey ->
                    backStackViewModel.settingsScreenKey = newKey
                },
                openLicenseScreen = openLicenseScreen,
                summitError = summitError,
                modifier = Modifier.fillMaxHeight()
            )
        }
    }
}

private val settingItems = listOf(
    CategoryItem(NormalNavKey.Settings.Renderer, { CategoryIcon(Icons.Outlined.VideoSettings, R.string.settings_tab_renderer) }, R.string.settings_tab_renderer),
    CategoryItem(NormalNavKey.Settings.Game, { CategoryIcon(Icons.Outlined.RocketLaunch, R.string.settings_tab_game) }, R.string.settings_tab_game),
    CategoryItem(NormalNavKey.Settings.Control, { CategoryIcon(Icons.Outlined.VideogameAsset, R.string.settings_tab_control) }, R.string.settings_tab_control),
    CategoryItem(NormalNavKey.Settings.Launcher, { CategoryIcon(R.drawable.ic_setting_launcher, R.string.settings_tab_launcher) }, R.string.settings_tab_launcher),
    CategoryItem(NormalNavKey.Settings.JavaManager, { CategoryIcon(R.drawable.ic_java, R.string.settings_tab_java_manage) }, R.string.settings_tab_java_manage, division = true),
    CategoryItem(NormalNavKey.Settings.ControlManager, { CategoryIcon(Icons.Outlined.VideogameAsset, R.string.settings_tab_control_manage) }, R.string.settings_tab_control_manage),
    CategoryItem(NormalNavKey.Settings.AboutInfo, { CategoryIcon(Icons.Outlined.Info, R.string.settings_tab_info_about) }, R.string.settings_tab_info_about, division = true)
)

@Composable
private fun TabMenu(
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    settingsScreenKey: NavKey?,
    navigateTo: (NavKey) -> Unit
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
                selected = settingsScreenKey === item.key,
                onClick = {
                    navigateTo(item.key)
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
    key: NestedNavKey.Settings,
    mainScreenKey: NavKey?,
    settingsScreenKey: NavKey?,
    onCurrentKeyChange: (NavKey?) -> Unit,
    openLicenseScreen: (raw: Int) -> Unit,
    summitError: (ErrorViewModel.ThrowableMessage) -> Unit,
    modifier: Modifier = Modifier
) {
    val backStack = key.backStack
    val stackTopKey = backStack.lastOrNull()
    LaunchedEffect(stackTopKey) {
        onCurrentKeyChange(stackTopKey)
    }

    if (backStack.isNotEmpty()) {
        NavDisplay(
            backStack = backStack,
            modifier = modifier,
            onBack = {
                onBack(backStack)
            },
            entryDecorators = listOf(
                rememberSceneSetupNavEntryDecorator(),
                rememberSavedStateNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            ),
            entryProvider = entryProvider {
                entry<NormalNavKey.Settings.Renderer> {
                    RendererSettingsScreen(key, settingsScreenKey, mainScreenKey)
                }
                entry<NormalNavKey.Settings.Game> {
                    GameSettingsScreen(key, settingsScreenKey, mainScreenKey)
                }
                entry<NormalNavKey.Settings.Control> {
                    ControlSettingsScreen(key, settingsScreenKey, mainScreenKey, summitError)
                }
                entry<NormalNavKey.Settings.Launcher> {
                    LauncherSettingsScreen(key, settingsScreenKey, mainScreenKey)
                }
                entry<NormalNavKey.Settings.JavaManager> {
                    JavaManageScreen(key, settingsScreenKey, mainScreenKey, summitError)
                }
                entry<NormalNavKey.Settings.ControlManager> {
                    ControlManageScreen(key, settingsScreenKey, mainScreenKey)
                }
                entry<NormalNavKey.Settings.AboutInfo> {
                    AboutInfoScreen(key, settingsScreenKey, mainScreenKey, openLicenseScreen)
                }
            }
        )
    } else {
        Box(modifier)
    }
}