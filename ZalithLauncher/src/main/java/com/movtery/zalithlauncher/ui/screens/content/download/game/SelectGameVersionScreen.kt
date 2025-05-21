package com.movtery.zalithlauncher.ui.screens.content.download.game

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.versioninfo.MinecraftVersions
import com.movtery.zalithlauncher.game.versioninfo.models.VersionManifest
import com.movtery.zalithlauncher.state.MutableStates
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.ContentCheckBox
import com.movtery.zalithlauncher.ui.components.LittleTextLabel
import com.movtery.zalithlauncher.ui.components.ScalingLabel
import com.movtery.zalithlauncher.ui.components.itemLayoutColor
import com.movtery.zalithlauncher.ui.screens.content.DOWNLOAD_SCREEN_TAG
import com.movtery.zalithlauncher.ui.screens.content.download.DOWNLOAD_GAME_SCREEN_TAG
import com.movtery.zalithlauncher.utils.animation.getAnimateTween
import com.movtery.zalithlauncher.utils.animation.swapAnimateDpAsState
import com.movtery.zalithlauncher.utils.formatDate
import com.movtery.zalithlauncher.utils.network.NetWorkUtils
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.ConnectException
import java.net.UnknownHostException
import java.nio.channels.UnresolvedAddressException

const val SELECT_GAME_VERSION_SCREEN_TAG = "SelectGameVersionScreen"

/** 版本列表加载状态 */
private sealed interface VersionState {
    /** 加载中 */
    data object Loading : VersionState
    /** 加载出现异常 */
    data class Failure(val message: Int, val args: Array<Any>? = null) : VersionState {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Failure

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

private data class VersionFilter(val release: Boolean, val snapshot: Boolean, val old: Boolean, val id: String = "")

@Composable
fun SelectGameVersionScreen(
    onVersionSelect: (String) -> Unit = {}
) {
    BaseScreen(
        Triple(DOWNLOAD_SCREEN_TAG, MutableStates.mainScreenTag, true),
        Triple(DOWNLOAD_GAME_SCREEN_TAG, MutableStates.downloadScreenTag, false),
        Triple(SELECT_GAME_VERSION_SCREEN_TAG, DownloadGameScreenStates.screenTag, false),
    ) { isVisible ->
        val yOffset by swapAnimateDpAsState(
            targetValue = (-40).dp,
            swapIn = isVisible
        )

        var reloadTrigger by remember { mutableStateOf(false) }
        var forceReload by remember { mutableStateOf(false) }

        var versionState by remember {
            mutableStateOf<VersionState?>(VersionState.Loading)
        }

        //简易版本类型过滤器
        var versionFilter by remember { mutableStateOf(VersionFilter(release = true, snapshot = false, old = false)) }

        var allVersions by remember { mutableStateOf<List<VersionManifest.Version>>(emptyList()) }
        var filteredVersions by remember { mutableStateOf<List<VersionManifest.Version>>(emptyList()) }

        Card(
            modifier = Modifier
                .padding(all = 12.dp)
                .fillMaxSize()
                .offset { IntOffset(x = 0, y = yOffset.roundToPx()) },
            shape = MaterialTheme.shapes.extraLarge
        ) {
            when (val state = versionState) {
                is VersionState.Loading -> {
                    Box(Modifier.fillMaxSize()) {
                        CircularProgressIndicator(Modifier.align(Alignment.Center))
                    }
                }

                is VersionState.Failure -> {
                    Box(Modifier.fillMaxSize()) {
                        val message = if (state.args != null) {
                            stringResource(state.message, *state.args)
                        } else {
                            stringResource(state.message)
                        }

                        ScalingLabel(
                            modifier = Modifier.align(Alignment.Center),
                            text = stringResource(R.string.download_game_failed_to_get_versions, message),
                            onClick = {
                                forceReload = true
                                reloadTrigger = !reloadTrigger
                            }
                        )
                    }
                }

                else -> {
                    VersionList(
                        itemContainerColor = itemLayoutColor(),
                        itemContentColor = MaterialTheme.colorScheme.onSurface,
                        versionFilter = versionFilter,
                        onVersionFilterChange = { versionFilter = it },
                        onRefreshClick = {
                            forceReload = true
                            reloadTrigger = !reloadTrigger
                        },
                        versions = filteredVersions,
                        onVersionSelect = onVersionSelect
                    )
                }
            }
        }

        LaunchedEffect(reloadTrigger) {
            versionState = VersionState.Loading
            versionState = runCatching {
                allVersions = MinecraftVersions.getVersionManifest(forceReload).versions
                filteredVersions = withContext(Dispatchers.Default) {
                    allVersions.filterVersions(versionFilter)
                }
                null
            }.getOrElse { e ->
                Log.w(SELECT_GAME_VERSION_SCREEN_TAG, "Failed to get version manifest!", e)
                val message: Pair<Int, Array<Any>?> = when(e) {
                    is HttpRequestTimeoutException -> R.string.error_timeout to null
                    is UnknownHostException, is UnresolvedAddressException -> R.string.error_network_unreachable to null
                    is ConnectException -> R.string.error_connection_failed to null
                    is ResponseException -> {
                        val statusCode = e.response.status
                        val res = when (statusCode) {
                            HttpStatusCode.Unauthorized -> R.string.error_unauthorized
                            HttpStatusCode.NotFound -> R.string.error_notfound
                            else -> R.string.error_client_error
                        }
                        res to arrayOf(statusCode)
                    }
                    else -> {
                        Log.e(SELECT_GAME_VERSION_SCREEN_TAG, "An unknown exception was caught!", e)
                        val errorMessage = e.localizedMessage ?: e.message ?: e::class.qualifiedName ?: "Unknown error"
                        R.string.error_unknown to arrayOf(errorMessage)
                    }
                }
                VersionState.Failure(message.first, message.second)
            }
        }

        LaunchedEffect(versionFilter) {
            if (allVersions.isNotEmpty()) {
                filteredVersions = withContext(Dispatchers.Default) {
                    allVersions.filterVersions(versionFilter)
                }
            }
        }
    }
}

/**
 * 简易过滤器，过滤特定类型的版本
 */
private fun List<VersionManifest.Version>.filterVersions(
    versionFilter: VersionFilter
) = this.filter {
    val type = when (it.type) {
        "release" -> versionFilter.release
        "snapshot" -> versionFilter.snapshot
        else -> versionFilter.old && it.type.startsWith("old")
    }
    val versionId = versionFilter.id
    val id = (versionId.isEmpty() || versionId.isBlank()) || it.id.contains(versionId)
    (type && id)
}

@Composable
private fun VersionList(
    itemContainerColor: Color,
    itemContentColor: Color,
    versionFilter: VersionFilter,
    onVersionFilterChange: (VersionFilter) -> Unit,
    onRefreshClick: () -> Unit,
    versions: List<VersionManifest.Version>,
    onVersionSelect: (String) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row {
                ContentCheckBox(
                    checked = versionFilter.release,
                    onCheckedChange = { onVersionFilterChange(versionFilter.copy(release = it)) }
                ) {
                    Text(
                        text = stringResource(R.string.download_game_type_release),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                ContentCheckBox(
                    checked = versionFilter.snapshot,
                    onCheckedChange = { onVersionFilterChange(versionFilter.copy(snapshot = it)) }
                ) {
                    Text(
                        text = stringResource(R.string.download_game_type_snapshot),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                ContentCheckBox(
                    checked = versionFilter.old,
                    onCheckedChange = { onVersionFilterChange(versionFilter.copy(old = it)) }
                ) {
                    Text(
                        text = stringResource(R.string.download_game_type_old),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            Spacer(modifier = Modifier.width(24.dp))

            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    color = itemContainerColor,
                    contentColor = itemContentColor,
                    shape = RoundedCornerShape(50f),
                    shadowElevation = 1.dp
                ) {
                    BasicTextField(
                        modifier = Modifier
                            .height(32.dp)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        value = versionFilter.id,
                        onValueChange = { onVersionFilterChange(versionFilter.copy(id = it)) },
                        textStyle = TextStyle(color = LocalContentColor.current).copy(fontSize = 12.sp),
                        cursorBrush = SolidColor(LocalTextSelectionColors.current.handleColor),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            Box(
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (versionFilter.id.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.generic_search),
                                        style = TextStyle(color = LocalContentColor.current).copy(fontSize = 12.sp)
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                IconButton(
                    onClick = onRefreshClick
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.generic_refresh)
                    )
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurface
        )

        val context = LocalContext.current

        LazyColumn(
            contentPadding = PaddingValues(all = 12.dp)
        ) {
            items(versions.size) { index ->
                val version = versions[index]

                VersionItemLayout(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = if (index != versions.size - 1) 12.dp else 0.dp),
                    version = version,
                    onClick = {
                        onVersionSelect(version.id)
                    },
                    onAccessWiki = { wikiUrl ->
                        NetWorkUtils.openLink(context, wikiUrl)
                    },
                    color = itemContainerColor,
                    contentColor = itemContentColor
                )
            }
        }
    }
}

@Composable
private fun VersionItemLayout(
    modifier: Modifier = Modifier,
    version: VersionManifest.Version,
    onClick: () -> Unit = {},
    onAccessWiki: (String) -> Unit = {},
    color: Color,
    contentColor: Color,
) {
    val scale = remember { Animatable(initialValue = 0.95f) }
    LaunchedEffect(Unit) {
        scale.animateTo(targetValue = 1f, animationSpec = getAnimateTween())
    }

    val (icon, versionType, wikiUrl) = getVersionComponents(version)

    Surface(
        modifier = modifier.graphicsLayer(scaleY = scale.value, scaleX = scale.value),
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = color,
        contentColor = contentColor,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .clip(shape = MaterialTheme.shapes.large)
                .padding(all = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let { versionIcon ->
                Image(
                    modifier = Modifier.size(32.dp),
                    painter = versionIcon,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = version.id,
                        style = MaterialTheme.typography.labelLarge
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    LittleTextLabel(
                        text = versionType
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = formatDate(input = version.releaseTime),
                    style = MaterialTheme.typography.labelMedium
                )
            }

            wikiUrl?.let { url ->
                IconButton(
                    modifier = Modifier.size(32.dp),
                    onClick = { onAccessWiki(url) }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Link,
                        contentDescription = "Wiki"
                    )
                }
            }
        }
    }
}

@Composable
private fun getVersionComponents(
    version: VersionManifest.Version
): Triple<Painter?, String, String?> {
    return when (version.type) {
        "release" -> {
            Triple(
                painterResource(R.drawable.ic_minecraft),
                stringResource(R.string.download_game_type_release),
                stringResource(R.string.url_wiki_minecraft_game_release, version.id)
            )
        }
        "snapshot" -> {
            Triple(
                painterResource(R.drawable.ic_command_block),
                stringResource(R.string.download_game_type_snapshot),
                stringResource(R.string.url_wiki_minecraft_game_snapshot, version.id)
            )
        }
        "old_beta" -> {
            Triple(
                painterResource(R.drawable.ic_old_cobblestone),
                stringResource(R.string.download_game_type_old_beta),
                null
            )
        }
        "old_alpha" -> {
            Triple(
                painterResource(R.drawable.ic_old_grass_block),
                stringResource(R.string.download_game_type_old_alpha),
                null
            )
        }
        else -> {
            Triple(
                null,
                stringResource(R.string.generic_unknown),
                null
            )
        }
    }
}