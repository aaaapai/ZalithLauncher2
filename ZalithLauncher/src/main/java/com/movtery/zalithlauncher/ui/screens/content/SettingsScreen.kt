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
import com.movtery.zalithlauncher.ui.screens.content.elements.CategoryIcon
import com.movtery.zalithlauncher.ui.screens.content.elements.CategoryItem
import com.movtery.zalithlauncher.ui.screens.content.settings.AboutInfoScreen
import com.movtery.zalithlauncher.ui.screens.content.settings.AboutInfoScreenKey
import com.movtery.zalithlauncher.ui.screens.content.settings.ControlManageScreen
import com.movtery.zalithlauncher.ui.screens.content.settings.ControlManageScreenKey
import com.movtery.zalithlauncher.ui.screens.content.settings.ControlSettingsScreen
import com.movtery.zalithlauncher.ui.screens.content.settings.ControlSettingsScreenKey
import com.movtery.zalithlauncher.ui.screens.content.settings.GameSettingsScreen
import com.movtery.zalithlauncher.ui.screens.content.settings.GameSettingsScreenKey
import com.movtery.zalithlauncher.ui.screens.content.settings.JavaManageScreen
import com.movtery.zalithlauncher.ui.screens.content.settings.JavaManageScreenKey
import com.movtery.zalithlauncher.ui.screens.content.settings.LauncherSettingsScreen
import com.movtery.zalithlauncher.ui.screens.content.settings.LauncherSettingsScreenKey
import com.movtery.zalithlauncher.ui.screens.content.settings.RendererSettingsScreen
import com.movtery.zalithlauncher.ui.screens.content.settings.RendererSettingsScreenKey
import com.movtery.zalithlauncher.ui.screens.main.elements.mainScreenKey
import com.movtery.zalithlauncher.ui.screens.navigateOnce
import com.movtery.zalithlauncher.utils.animation.swapAnimateDpAsState
import kotlinx.serialization.Serializable

@Serializable
data object SettingsScreenKey: NestedNavKey {
    override fun isLastScreen(): Boolean = settingsBackStack.size <= 1
}

/**
 * 设置屏幕堆栈
 */
val settingsBackStack = mutableStateListOf<NavKey>(RendererSettingsScreenKey)

/**
 * 状态：当前设置屏幕的标签
 */
var settingsScreenKey by mutableStateOf<NavKey?>(null)

@Composable
fun SettingsScreen() {
    BaseScreen(
        screenKey = SettingsScreenKey,
        currentKey = mainScreenKey
    ) { isVisible ->

        Row(modifier = Modifier.fillMaxSize()) {
            TabMenu(
                isVisible = isVisible,
                backStack = settingsBackStack,
                modifier = Modifier.fillMaxHeight()
            )
            NavigationUI(
                backStack = settingsBackStack,
                modifier = Modifier.fillMaxHeight()
            )
        }
    }
}

private val settingItems = listOf(
    CategoryItem(RendererSettingsScreenKey, { CategoryIcon(Icons.Outlined.VideoSettings, R.string.settings_tab_renderer) }, R.string.settings_tab_renderer),
    CategoryItem(GameSettingsScreenKey, { CategoryIcon(Icons.Outlined.RocketLaunch, R.string.settings_tab_game) }, R.string.settings_tab_game),
    CategoryItem(ControlSettingsScreenKey, { CategoryIcon(Icons.Outlined.VideogameAsset, R.string.settings_tab_control) }, R.string.settings_tab_control),
    CategoryItem(LauncherSettingsScreenKey, { CategoryIcon(R.drawable.ic_setting_launcher, R.string.settings_tab_launcher) }, R.string.settings_tab_launcher),
    CategoryItem(JavaManageScreenKey, { CategoryIcon(R.drawable.ic_java, R.string.settings_tab_java_manage) }, R.string.settings_tab_java_manage, division = true),
    CategoryItem(ControlManageScreenKey, { CategoryIcon(Icons.Outlined.VideogameAsset, R.string.settings_tab_control_manage) }, R.string.settings_tab_control_manage),
    CategoryItem(AboutInfoScreenKey, { CategoryIcon(Icons.Outlined.Info, R.string.settings_tab_info_about) }, R.string.settings_tab_info_about, division = true)
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
        settingsScreenKey = currentKey
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
            entry<RendererSettingsScreenKey> { RendererSettingsScreen() }
            entry<GameSettingsScreenKey> { GameSettingsScreen() }
            entry<ControlSettingsScreenKey> { ControlSettingsScreen() }
            entry<LauncherSettingsScreenKey> { LauncherSettingsScreen() }
            entry<JavaManageScreenKey> { JavaManageScreen() }
            entry<ControlManageScreenKey> { ControlManageScreen() }
            entry<AboutInfoScreenKey> { AboutInfoScreen() }
        }
    )
}