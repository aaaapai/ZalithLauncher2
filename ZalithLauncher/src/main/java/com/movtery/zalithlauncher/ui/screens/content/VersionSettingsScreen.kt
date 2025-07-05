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
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.content.elements.CategoryIcon
import com.movtery.zalithlauncher.ui.screens.content.elements.CategoryItem
import com.movtery.zalithlauncher.ui.screens.content.versions.ResourcePackManageScreen
import com.movtery.zalithlauncher.ui.screens.content.versions.ResourcePackManageScreenKey
import com.movtery.zalithlauncher.ui.screens.content.versions.SavesManagerScreen
import com.movtery.zalithlauncher.ui.screens.content.versions.SavesManagerScreenKey
import com.movtery.zalithlauncher.ui.screens.content.versions.VersionConfigScreen
import com.movtery.zalithlauncher.ui.screens.content.versions.VersionConfigScreenKey
import com.movtery.zalithlauncher.ui.screens.content.versions.VersionOverViewScreen
import com.movtery.zalithlauncher.ui.screens.content.versions.VersionOverViewScreenKey
import com.movtery.zalithlauncher.ui.screens.main.elements.mainScreenKey
import com.movtery.zalithlauncher.ui.screens.navigateOnce
import com.movtery.zalithlauncher.utils.animation.swapAnimateDpAsState
import kotlinx.serialization.Serializable

@Serializable
data object VersionSettingsScreenKey: NestedNavKey {
    override fun isLastScreen(): Boolean = true
}

/**
 * 状态：版本设置屏幕的标签
 */
var versionSettScreenKey by mutableStateOf<NavKey?>(null)

@Composable
fun VersionSettingsScreen() {
    val backStack = rememberNavBackStack(VersionOverViewScreenKey)

    BaseScreen(
        screenKey = VersionSettingsScreenKey,
        currentKey = mainScreenKey
    ) { isVisible ->

        Row(modifier = Modifier.fillMaxSize()) {
            TabMenu(
                isVisible = isVisible,
                backStack = backStack,
                modifier = Modifier.fillMaxHeight()
            )

            NavigationUI(
                backStack = backStack,
                modifier = Modifier.fillMaxHeight()
            )
        }
    }
}

private val settingItems = listOf(
    CategoryItem(VersionOverViewScreenKey, { CategoryIcon(Icons.Outlined.Dashboard, R.string.versions_settings_overview) }, R.string.versions_settings_overview),
    CategoryItem(VersionConfigScreenKey, { CategoryIcon(Icons.Outlined.Build, R.string.versions_settings_config) }, R.string.versions_settings_config),
    CategoryItem(SavesManagerScreenKey, { CategoryIcon(Icons.Outlined.Public, R.string.saves_manage) }, R.string.saves_manage, division = true),
    CategoryItem(ResourcePackManageScreenKey, { CategoryIcon(Icons.Outlined.Image, R.string.resource_pack_manage) }, R.string.resource_pack_manage)
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
                selected = versionSettScreenKey === item.key,
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
        versionSettScreenKey = currentKey
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
            entry<VersionOverViewScreenKey> { VersionOverViewScreen() }
            entry<VersionConfigScreenKey> { VersionConfigScreen() }
            entry<SavesManagerScreenKey> { SavesManagerScreen() }
            entry<ResourcePackManageScreenKey> { ResourcePackManageScreen() }
        }
    )
}