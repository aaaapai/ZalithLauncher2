package com.movtery.zalithlauncher.ui.screens.content.download.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.addons.modloader.ModLoader
import com.movtery.zalithlauncher.game.addons.modloader.fabriclike.FabricLikeVersion
import com.movtery.zalithlauncher.game.addons.modloader.forgelike.forge.ForgeVersion
import com.movtery.zalithlauncher.game.addons.modloader.forgelike.neoforge.NeoForgeVersion
import com.movtery.zalithlauncher.game.addons.modloader.optifine.OptiFineVersion
import com.movtery.zalithlauncher.ui.components.itemLayoutColor
import com.movtery.zalithlauncher.utils.animation.getAnimateTween

/** Addon 加载状态 */
sealed interface AddonState {
    /** 已完成加载 */
    data object None : AddonState
    /** 加载中 */
    data object Loading : AddonState
    /**
     * 加载出现异常
     * @param message 异常消息资源
     * @param args 消息参数
     */
    data class Error(val message: Int, val args: Array<Any>? = null): AddonState {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Error

            if (message != other.message) return false
            if (args != null) {
                if (other.args == null) return false
                if (!args.contentEquals(other.args)) return false
            } else if (other.args != null) return false

            return true
        }

        override fun hashCode(): Int {
            var result = message
            result = 31 * result + (args?.contentHashCode() ?: 0)
            return result
        }
    }
}

/**
 * 简易 Addon 文本占位
 */
@Composable
private fun AddonTextLayout(
    modifier: Modifier = Modifier,
    title: String,
    summary: String
) {
    Column(
        modifier = modifier.padding(all = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(modifier = Modifier.height(height = 4.dp))
        Text(
            text = summary,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

/**
 * Addon 版本列表
 * @param state Addon 当前的加载状态
 * @param items Addon 版本列表
 * @param current 当前选择的 Addon 版本
 * @param incompatibleSet 当前 Addon 的不兼容列表
 * @param checkIncompatible 检查当前的 Addon 的不兼容情况
 * @param triggerCheckIncompatible 手动触发检查不兼容情况
 * @param error 设置错误名称，让该列表不可用
 * @param iconPainter Addon 的图标
 * @param maxListHeight 列表展开最高显示高度
 * @param autoCollapse 选择版本后是否自动收起
 */
@Composable
fun <E> AddonListLayout(
    modifier: Modifier = Modifier,
    state: AddonState,
    items: List<E>?,
    current: E?,
    incompatibleSet: Set<ModLoader>,
    checkIncompatible: () -> Unit = {},
    triggerCheckIncompatible: Array<Any> = arrayOf(Unit),
    error: String? = null,
    iconPainter: Painter,
    title: String,
    getItemText: @Composable (E) -> String,
    summary: (@Composable (E) -> Unit)? = null,
    maxListHeight: Dp = 200.dp,
    autoCollapse: Boolean = true,
    onValueChange: (E?) -> Unit = {},
    onReload: () -> Unit = {},
    color: Color = itemLayoutColor(),
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    var selectedItem by remember { mutableStateOf<E?>(null) }

    LaunchedEffect(state, selectedItem, *triggerCheckIncompatible) {
        //加载状态 || 选择项变更 || 手动触发，检查一次不兼容情况
        checkIncompatible()
    }

    LaunchedEffect(state, items, current) {
        selectedItem = items?.firstOrNull { it == current }
    }
    var expanded by remember { mutableStateOf(false) }

    fun clear() {
        onValueChange(null)
        selectedItem = null
        if (autoCollapse) expanded = false
    }

    //不兼容列表不为空，清除当前Addon的选择
    LaunchedEffect(incompatibleSet) {
        if (incompatibleSet.isNotEmpty()) clear()
    }

    Surface(
        modifier = modifier.padding(bottom = 12.dp),
        shape = MaterialTheme.shapes.large,
        color = color,
        contentColor = contentColor,
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            AddonListHeader(
                modifier = Modifier.fillMaxWidth(),
                state = state,
                iconPainter = iconPainter,
                title = title,
                items = items,
                selectedItem = selectedItem,
                getItemText = getItemText,
                incompatibleSet = incompatibleSet,
                error = error,
                expanded = expanded,
                //加载已完成 && 版本列表不为空 && 不兼容列表为空 && 错误名称为 null -> 可展开
                enabled = state == AddonState.None && !items.isNullOrEmpty() && incompatibleSet.isEmpty() && error == null,
                onClick = { expanded = !expanded },
                onClear = ::clear,
                onReload = onReload
            )

            if (state == AddonState.None && !items.isNullOrEmpty() && error == null) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    AnimatedVisibility(
                        visible = expanded,
                        enter = expandVertically(animationSpec = getAnimateTween()),
                        exit = shrinkVertically(animationSpec = getAnimateTween()) + fadeOut(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = maxListHeight)
                                .padding(vertical = 4.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            items(items.size) { index ->
                                val item = items[index]
                                AddonListItem(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(all = 4.dp),
                                    selected = selectedItem == item,
                                    itemName = getItemText(item),
                                    summary = summary?.let {
                                        { it.invoke(item) }
                                    },
                                    onClick = {
                                        if (expanded && selectedItem != item) {
                                            onValueChange(item)
                                            selectedItem = item
                                            if (autoCollapse) expanded = false
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun <E> AddonListHeader(
    modifier: Modifier = Modifier,
    state: AddonState,
    iconPainter: Painter,
    title: String,
    items: List<E>?,
    selectedItem: E?,
    getItemText: @Composable (E) -> String,
    incompatibleSet: Set<ModLoader>,
    error: String? = null,
    expanded: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
    onClear: () -> Unit = {},
    onReload: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(16.dp))
        Image(
            modifier = Modifier.size(34.dp),
            painter = iconPainter,
            contentDescription = null
        )
        Spacer(modifier = Modifier.width(8.dp))

        when(state) {
            is AddonState.None -> {
                Column(modifier = Modifier.weight(1f)) {
                    val summary: String = when {
                        error != null -> error
                        incompatibleSet.isNotEmpty() -> {
                            stringResource(R.string.download_game_addon_incompatible_with, incompatibleSet.joinToString(", ") { it.displayName })
                        }
                        items.isNullOrEmpty() -> stringResource(R.string.download_game_addon_unavailable)
                        selectedItem == null -> stringResource(R.string.download_game_addon_available)
                        else -> stringResource(R.string.settings_element_selected, getItemText(selectedItem))
                    }
                    AddonTextLayout(
                        modifier = Modifier.fillMaxWidth(),
                        title = title,
                        summary = summary,
                    )
                }
                if (!items.isNullOrEmpty() && incompatibleSet.isEmpty() && error == null) {
                    val rotation by animateFloatAsState(
                        targetValue = if (expanded) -180f else 0f,
                        animationSpec = getAnimateTween()
                    )
                    Icon(
                        modifier = Modifier
                            .size(34.dp)
                            .rotate(rotation),
                        imageVector = Icons.Rounded.ArrowDropDown,
                        contentDescription = stringResource(if (expanded) R.string.generic_expand else R.string.generic_collapse)
                    )
                    if (selectedItem != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            modifier = Modifier
                                .size(34.dp),
                            onClick = onClear
                        ) {
                            Icon(
                                modifier = Modifier.size(24.dp),
                                imageVector = Icons.Outlined.Clear,
                                contentDescription = stringResource(R.string.generic_clear)
                            )
                        }
                    }
                }
            }
            is AddonState.Loading -> {
                AddonTextLayout(
                    modifier = Modifier.weight(1f),
                    title = title,
                    summary = stringResource(R.string.generic_loading),
                )
            }
            is AddonState.Error -> {
                val message = if (state.args != null) {
                    stringResource(state.message, *state.args)
                } else {
                    stringResource(state.message)
                }

                AddonTextLayout(
                    modifier = Modifier.weight(1f),
                    title = title,
                    summary = stringResource(R.string.download_game_addon_list_load_error, message),
                )
                IconButton(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .size(34.dp),
                    onClick = onReload
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.generic_refresh)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))
    }
}

@Composable
fun AddonListItem(
    modifier: Modifier = Modifier,
    selected: Boolean,
    itemName: String,
    summary: (@Composable () -> Unit)? = null,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .clip(shape = MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Column {
            Text(
                text = itemName,
                style = MaterialTheme.typography.labelMedium
            )
            summary?.let {
                Spacer(
                    modifier = Modifier.height(height = 4.dp)
                )
                it()
            }
        }
    }
}

@Composable
fun OptiFineVersionSummary(optifine: OptiFineVersion) {
    val typeText = if (optifine.isPreview) {
        stringResource(R.string.download_game_addon_preview)
    } else {
        stringResource(R.string.download_game_addon_release)
    }

    val dateText = stringResource(R.string.download_game_addon_date, optifine.releaseDate)

    val compatibilityText = when {
        optifine.forgeVersion == null -> {
            stringResource(R.string.download_game_addon_incompatible_with, ModLoader.FORGE.displayName)
        }
        optifine.forgeVersion.isNotEmpty() -> {
            stringResource(R.string.download_game_addon_compatible_with, "${ModLoader.FORGE.displayName} ${optifine.forgeVersion}")
        }
        else -> null
    }

    Row(
        modifier = Modifier.alpha(alpha = 0.7f)
    ) {
        Text(text = typeText, style = MaterialTheme.typography.labelSmall)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = dateText, style = MaterialTheme.typography.labelSmall)
        compatibilityText?.let {
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = it, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun ForgeVersionSummary(forgeVersion: ForgeVersion) {
    val recommendedText = if (forgeVersion.isRecommended) {
        stringResource(R.string.download_game_addon_recommended, ModLoader.FORGE.displayName)
    } else null

    val dateText = stringResource(R.string.download_game_addon_date, forgeVersion.releaseTime)

    Row(
        modifier = Modifier.alpha(alpha = 0.7f)
    ) {
        recommendedText?.let {
            Text(text = it, style = MaterialTheme.typography.labelSmall)
            Spacer(modifier = Modifier.width(12.dp))
        }
        Text(text = dateText, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun NeoForgeSummary(neoforgeVersion: NeoForgeVersion) {
    val typeText = if (neoforgeVersion.isBeta) {
        stringResource(R.string.download_game_addon_debug)
    } else {
        stringResource(R.string.download_game_addon_release)
    }

    Row(
        modifier = Modifier.alpha(alpha = 0.7f)
    ) {
        Text(text = typeText, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun FabricLikeSummary(fabricLikeVersion: FabricLikeVersion) {
    val typeText = if (fabricLikeVersion.stable) {
        stringResource(R.string.download_game_addon_stable)
    } else {
        stringResource(R.string.download_game_addon_debug)
    }

    Row(
        modifier = Modifier.alpha(alpha = 0.7f)
    ) {
        Text(text = typeText, style = MaterialTheme.typography.labelSmall)
    }
}