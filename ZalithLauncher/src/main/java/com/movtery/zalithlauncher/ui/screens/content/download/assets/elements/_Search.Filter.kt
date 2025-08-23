package com.movtery.zalithlauncher.ui.screens.content.download.assets.elements

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.download.assets.platform.Platform
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformDisplayLabel
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformFilterCode
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformSortField
import com.movtery.zalithlauncher.game.download.assets.utils.allGameVersions
import com.movtery.zalithlauncher.ui.components.LittleTextLabel
import com.movtery.zalithlauncher.ui.components.itemLayoutColor
import com.movtery.zalithlauncher.utils.animation.getAnimateTween

/**
 * 搜索资源过滤器UI
 * @param enablePlatform 是否允许更改目标平台
 * @param searchPlatform 目标平台
 * @param searchName 搜索名称
 * @param gameVersion 游戏版本
 * @param sortField 排序方式
 * @param categories 可用资源类别列表
 * @param category 资源类别
 * @param enableModLoader 是否启用模组加载器过滤
 * @param modloaders 可用模组加载器列表
 * @param modloader 模组加载器
 */
@Composable
fun SearchFilter(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    enablePlatform: Boolean = true,
    searchPlatform: Platform,
    onPlatformChange: (Platform) -> Unit = {},
    searchName: String,
    onSearchNameChange: (String) -> Unit = {},
    gameVersion: String?,
    onGameVersionChange: (String?) -> Unit = {},
    sortField: PlatformSortField,
    onSortFieldChange: (PlatformSortField) -> Unit = {},
    categories: List<PlatformFilterCode>,
    category: PlatformFilterCode?,
    onCategoryChange: (PlatformFilterCode?) -> Unit = {},
    enableModLoader: Boolean = true,
    modloaders: List<PlatformDisplayLabel> = emptyList(),
    modloader: PlatformDisplayLabel? = null,
    onModLoaderChange: (PlatformDisplayLabel?) -> Unit = {}
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = searchName,
                onValueChange = onSearchNameChange,
                shape = MaterialTheme.shapes.large,
                label = {
                    Text(text = stringResource(R.string.download_assets_filter_search_name))
                },
                singleLine = true
            )
        }

        if (enablePlatform) {
            item {
                FilterListLayout(
                    modifier = Modifier.fillMaxWidth(),
                    items = Platform.entries,
                    selectedItem = searchPlatform,
                    onItemSelected = {
                        onPlatformChange(it!!)
                    },
                    selectedLayout = { platform ->
                        platform?.let {
                            PlatformIdentifier(
                                platform = it,
                                shape = MaterialTheme.shapes.small
                            )
                        }
                    },
                    itemLayout = { platform ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                modifier = Modifier.size(14.dp),
                                painter = painterResource(platform.getDrawable()),
                                contentDescription = platform.displayName
                            )
                            Text(
                                text = platform.displayName,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    },
                    title = stringResource(R.string.download_assets_filter_search_platform),
                    cancelable = false
                )
            }
        }

        item {
            FilterListLayout(
                modifier = Modifier.fillMaxWidth(),
                items = allGameVersions,
                selectedItem = gameVersion,
                onItemSelected = {
                    onGameVersionChange(it)
                },
                getItemName = { it },
                title = stringResource(R.string.download_assets_filter_game_version)
            )
        }

        item {
            FilterListLayout(
                modifier = Modifier.fillMaxWidth(),
                items = PlatformSortField.entries,
                selectedItem = sortField,
                onItemSelected = {
                    onSortFieldChange(it!!)
                },
                getItemName = {
                    stringResource(it.getDisplayName())
                },
                title = stringResource(R.string.download_assets_filter_sort_field),
                cancelable = false
            )
        }

        item {
            FilterListLayout(
                modifier = Modifier.fillMaxWidth(),
                items = categories,
                selectedItem = category,
                onItemSelected = {
                    onCategoryChange(it)
                },
                getItemName = {
                    stringResource(it.getDisplayName())
                },
                title = stringResource(R.string.download_assets_filter_category)
            )
        }

        if (enableModLoader) {
            item {
                FilterListLayout(
                    modifier = Modifier.fillMaxWidth(),
                    items = modloaders,
                    selectedItem = modloader,
                    onItemSelected = {
                        onModLoaderChange(it)
                    },
                    getItemName = {
                        it.getDisplayName()
                    },
                    title = stringResource(R.string.download_assets_filter_modloader)
                )
            }
        }
    }
}

/**
 * 过滤器列表UI
 * @param items 可选的item
 * @param selectedItem 当前选中的item
 * @param onItemSelected 选中item时的回调
 * @param getItemName 获取item的显示名称
 * @param cancelable 是否允许取消选择（清除已选择的item）
 */
@Composable
private fun <E> FilterListLayout(
    modifier: Modifier = Modifier,
    items: List<E>,
    selectedItem: E?,
    onItemSelected: (E?) -> Unit,
    getItemName: @Composable (E) -> String,
    title: String,
    cancelable: Boolean = true,
    maxListHeight: Dp = 200.dp,
    shape: Shape = MaterialTheme.shapes.large,
    color: Color = itemLayoutColor(),
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    shadowElevation: Dp = 1.dp
) {
    FilterListLayout(
        modifier = modifier,
        items = items,
        selectedItem = selectedItem,
        onItemSelected = onItemSelected,
        selectedLayout = { item ->
            LittleTextLabel(
                text = item?.let { getItemName(it) } ?: stringResource(R.string.download_assets_filter_none),
                shape = MaterialTheme.shapes.small
            )
        },
        itemLayout = { item ->
            Text(
                text = getItemName(item),
                style = MaterialTheme.typography.labelMedium
            )
        },
        title = title,
        cancelable = cancelable,
        maxListHeight = maxListHeight,
        shape = shape,
        color = color,
        contentColor = contentColor,
        shadowElevation = shadowElevation
    )
}

/**
 * 过滤器列表UI
 * @param items 可选的item
 * @param selectedItem 当前选中的item
 * @param onItemSelected 选中item时的回调
 * @param selectedLayout 控制item的显示外观
 * @param cancelable 是否允许取消选择（清除已选择的item）
 */
@Composable
private fun <E> FilterListLayout(
    modifier: Modifier = Modifier,
    items: List<E>,
    selectedItem: E?,
    onItemSelected: (E?) -> Unit,
    selectedLayout: @Composable ColumnScope.(E?) -> Unit,
    itemLayout: @Composable ColumnScope.(E) -> Unit,
    title: String,
    cancelable: Boolean = true,
    maxListHeight: Dp = 200.dp,
    shape: Shape = MaterialTheme.shapes.large,
    color: Color = itemLayoutColor(),
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    shadowElevation: Dp = 1.dp
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier,
        shape = shape,
        color = color,
        contentColor = contentColor,
        shadowElevation = shadowElevation
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            FilterListHeader(
                modifier = Modifier.fillMaxWidth(),
                items = items,
                title = title,
                selected = selectedItem != null,
                selectedItemLayout = { selectedLayout(selectedItem) },
                expanded = expanded,
                cancelable = cancelable,
                onClick = { expanded = !expanded },
                onClear = { onItemSelected(null) }
            )

            if (items.isNotEmpty()) {
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
                            items(items) { item ->
                                FilterListItem(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(all = 4.dp),
                                    selected = selectedItem == item,
                                    itemLayout = { itemLayout(item) },
                                    onClick = {
                                        if (expanded && selectedItem != item) {
                                            onItemSelected(item)
                                            expanded = false
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
private fun <E> FilterListHeader(
    modifier: Modifier = Modifier,
    items: List<E>,
    title: String,
    selected: Boolean,
    selectedItemLayout: @Composable ColumnScope.() -> Unit,
    expanded: Boolean,
    cancelable: Boolean = true,
    onClick: () -> Unit = {},
    onClear: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall
            )
            selectedItemLayout()
        }

        if (!items.isEmpty()) {
            Row(
                modifier = Modifier.padding(end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val rotation by animateFloatAsState(
                    targetValue = if (expanded) -180f else 0f,
                    animationSpec = getAnimateTween()
                )
                Icon(
                    modifier = Modifier
                        .size(28.dp)
                        .rotate(rotation),
                    imageVector = Icons.Rounded.ArrowDropDown,
                    contentDescription = stringResource(if (expanded) R.string.generic_expand else R.string.generic_collapse)
                )
                if (selected && cancelable) {
                    IconButton(
                        modifier = Modifier
                            .size(28.dp),
                        onClick = onClear
                    ) {
                        Icon(
                            modifier = Modifier.size(18.dp),
                            imageVector = Icons.Outlined.Clear,
                            contentDescription = stringResource(R.string.generic_clear)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterListItem(
    modifier: Modifier = Modifier,
    selected: Boolean,
    itemLayout: @Composable ColumnScope.() -> Unit,
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
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            content = itemLayout
        )
    }
}