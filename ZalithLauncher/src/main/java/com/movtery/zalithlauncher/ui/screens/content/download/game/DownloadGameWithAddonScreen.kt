package com.movtery.zalithlauncher.ui.screens.content.download.game

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.addons.modloader.ModLoader
import com.movtery.zalithlauncher.game.addons.modloader.ResponseTooShortException
import com.movtery.zalithlauncher.game.addons.modloader.fabriclike.fabric.FabricAPIVersions
import com.movtery.zalithlauncher.game.addons.modloader.fabriclike.fabric.FabricVersion
import com.movtery.zalithlauncher.game.addons.modloader.fabriclike.fabric.FabricVersions
import com.movtery.zalithlauncher.game.addons.modloader.fabriclike.quilt.QuiltAPIVersions
import com.movtery.zalithlauncher.game.addons.modloader.fabriclike.quilt.QuiltVersion
import com.movtery.zalithlauncher.game.addons.modloader.fabriclike.quilt.QuiltVersions
import com.movtery.zalithlauncher.game.addons.modloader.forgelike.forge.ForgeVersion
import com.movtery.zalithlauncher.game.addons.modloader.forgelike.forge.ForgeVersions
import com.movtery.zalithlauncher.game.addons.modloader.forgelike.neoforge.NeoForgeVersion
import com.movtery.zalithlauncher.game.addons.modloader.forgelike.neoforge.NeoForgeVersions
import com.movtery.zalithlauncher.game.addons.modloader.modlike.ModVersion
import com.movtery.zalithlauncher.game.addons.modloader.optifine.OptiFineVersion
import com.movtery.zalithlauncher.game.addons.modloader.optifine.OptiFineVersions
import com.movtery.zalithlauncher.game.download.game.GameDownloadInfo
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.SimpleTextInputField
import com.movtery.zalithlauncher.ui.components.itemLayoutColor
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.content.elements.isFilenameInvalid
import com.movtery.zalithlauncher.utils.animation.getAnimateTween
import com.movtery.zalithlauncher.utils.animation.swapAnimateDpAsState
import com.movtery.zalithlauncher.utils.logging.Logger.lError
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import java.net.ConnectException
import java.net.UnknownHostException
import java.nio.channels.UnresolvedAddressException

private class AddonList {
    //版本列表
    var optifineList by mutableStateOf<List<OptiFineVersion>?>(null)
    var forgeList by mutableStateOf<List<ForgeVersion>?>(null)
    var neoforgeList by mutableStateOf<List<NeoForgeVersion>?>(null)
    var fabricList by mutableStateOf<List<FabricVersion>?>(null)
    var fabricAPIList by mutableStateOf<List<ModVersion>?>(null)
    var quiltList by mutableStateOf<List<QuiltVersion>?>(null)
    var quiltAPIList by mutableStateOf<List<ModVersion>?>(null)
}

private class CurrentAddon {
    //当前选择版本
    var optifineVersion by mutableStateOf<OptiFineVersion?>(null)
    var forgeVersion by mutableStateOf<ForgeVersion?>(null)
    var neoforgeVersion by mutableStateOf<NeoForgeVersion?>(null)
    var fabricVersion by mutableStateOf<FabricVersion?>(null)
    var fabricAPIVersion by mutableStateOf<ModVersion?>(null)
    var quiltVersion by mutableStateOf<QuiltVersion?>(null)
    var quiltAPIVersion by mutableStateOf<ModVersion?>(null)

    //加载状态
    var optifineState by mutableStateOf<AddonState>(AddonState.None)
    var forgeState by mutableStateOf<AddonState>(AddonState.None)
    var neoforgeState by mutableStateOf<AddonState>(AddonState.None)
    var fabricState by mutableStateOf<AddonState>(AddonState.None)
    var fabricAPIState by mutableStateOf<AddonState>(AddonState.None)
    var quiltState by mutableStateOf<AddonState>(AddonState.None)
    var quiltAPIState by mutableStateOf<AddonState>(AddonState.None)

    //不兼容列表 利用Set集合不可重复
    var incompatibleWithOptiFine by mutableStateOf<Set<ModLoader>>(emptySet())
    var incompatibleWithForge by mutableStateOf<Set<ModLoader>>(emptySet())
    var incompatibleWithNeoForge by mutableStateOf<Set<ModLoader>>(emptySet())
    var incompatibleWithFabric by mutableStateOf<Set<ModLoader>>(emptySet())
    var incompatibleWithFabricAPI by mutableStateOf<Set<ModLoader>>(emptySet())
    var incompatibleWithQuilt by mutableStateOf<Set<ModLoader>>(emptySet())
    var incompatibleWithQuiltAPI by mutableStateOf<Set<ModLoader>>(emptySet())
}

private class AddonsViewModel(
    private val gameVersion: String
) : ViewModel() {
    val addonList = AddonList()
    val currentAddon = CurrentAddon()
    var refreshIcon by mutableStateOf(false)
        private set

    fun refreshIcon() {
        refreshIcon = !refreshIcon
    }

    fun reloadOptiFine() = launchAddonReload(
        { currentAddon.optifineState = it },
        { OptiFineVersions.fetchOptiFineList()?.filter { it.inherit == gameVersion } },
        { addonList.optifineList = it }
    )

    fun reloadForge() = launchAddonReload(
        { currentAddon.forgeState = it },
        { ForgeVersions.fetchForgeList(gameVersion) },
        { addonList.forgeList = it }
    )

    fun reloadNeoForge() = launchAddonReload(
        { currentAddon.neoforgeState = it },
        { NeoForgeVersions.fetchNeoForgeList()?.filter { it.inherit == gameVersion } },
        { addonList.neoforgeList = it }
    )

    fun reloadFabric() = launchAddonReload(
        { currentAddon.fabricState = it },
        { FabricVersions.fetchFabricLoaderList(gameVersion) },
        { addonList.fabricList = it }
    )

    fun reloadFabricAPI() = launchAddonReload(
        { currentAddon.fabricAPIState = it },
        { FabricAPIVersions.fetchVersionList(gameVersion) },
        { addonList.fabricAPIList = it }
    )

    fun reloadQuilt() = launchAddonReload(
        { currentAddon.quiltState = it },
        { QuiltVersions.fetchQuiltLoaderList(gameVersion) },
        { addonList.quiltList = it }
    )

    fun reloadQuiltAPI() = launchAddonReload(
        { currentAddon.quiltAPIState = it },
        { QuiltAPIVersions.fetchVersionList(gameVersion) },
        { addonList.quiltAPIList = it }
    )

    private fun <T> launchAddonReload(
        updateState: (AddonState) -> Unit,
        fetch: suspend () -> T?,
        onSuccess: (T?) -> Unit
    ) {
        viewModelScope.launch {
            runWithState(updateState, fetch).also(onSuccess)
        }
    }

    private suspend fun <T> runWithState(
        updateState: (AddonState) -> Unit,
        block: suspend () -> T?
    ): T? {
        updateState(AddonState.Loading)
        return runCatching {
            block().also {
                updateState(AddonState.None)
            }
        }.onFailure { e ->
            val state = when (e) {
                is ResponseTooShortException -> {
                    //忽略，判定为不可用
                    AddonState.None
                }
                is HttpRequestTimeoutException -> AddonState.Error(R.string.error_timeout)
                is UnknownHostException, is UnresolvedAddressException -> {
                    AddonState.Error(R.string.error_network_unreachable)
                }
                is ConnectException -> {
                    AddonState.Error(R.string.error_connection_failed)
                }
                is SerializationException -> {
                    AddonState.Error(R.string.error_parse_failed)
                }
                is ResponseException -> {
                    val statusCode = e.response.status
                    val res = when (statusCode) {
                        HttpStatusCode.Unauthorized -> R.string.error_unauthorized
                        HttpStatusCode.NotFound -> R.string.error_notfound
                        else -> R.string.error_client_error
                    }
                    AddonState.Error(res, arrayOf(statusCode))
                }
                else -> {
                    lError("An unknown exception was caught!", e)
                    val errorMessage = e.localizedMessage ?: e.message ?: e::class.qualifiedName ?: "Unknown error"
                    AddonState.Error(R.string.error_unknown, arrayOf(errorMessage))
                }
            }
            updateState(state)
        }.getOrNull()
    }

    init {
        reloadOptiFine()
        reloadForge()
        reloadNeoForge()
        reloadFabric()
        reloadFabricAPI()
        reloadQuilt()
        reloadQuiltAPI()
    }

    override fun onCleared() {
        viewModelScope.cancel()
    }
}

@Composable
fun DownloadGameWithAddonScreen(
    mainScreenKey: NavKey?,
    downloadScreenKey: NavKey?,
    downloadGameScreenKey: NavKey?,
    key: NormalNavKey.DownloadGame.Addons,
    onInstall: (GameDownloadInfo) -> Unit = {}
) {
    val viewModel = viewModel(
        key = key.toString()
    ) {
        AddonsViewModel(key.gameVersion)
    }

    BaseScreen(
        listOf(
            Pair(NestedNavKey.Download::class.java, mainScreenKey),
            Pair(NestedNavKey.DownloadGame::class.java, downloadScreenKey),
            Pair(NormalNavKey.DownloadGame.Addons::class.java, downloadGameScreenKey)
        )
    ) { isVisible ->
        val yOffset by swapAnimateDpAsState(
            targetValue = (-40).dp,
            swapIn = isVisible
        )

        Card(
            modifier = Modifier
                .padding(all = 12.dp)
                .fillMaxSize()
                .offset { IntOffset(x = 0, y = yOffset.roundToPx()) },
            shape = MaterialTheme.shapes.extraLarge
        ) {
            val itemContainerColor = itemLayoutColor()
            val itemContentColor = MaterialTheme.colorScheme.onSurface

            ScreenHeader(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                itemContainerColor = itemContainerColor,
                itemContentColor = itemContentColor,
                gameVersion = key.gameVersion,
                currentAddon = viewModel.currentAddon,
                refreshIcon = viewModel.refreshIcon,
                onInstall = { customVersionName ->
                    onInstall(
                        GameDownloadInfo(
                            gameVersion = key.gameVersion,
                            customVersionName = customVersionName,
                            optifine = viewModel.currentAddon.optifineVersion,
                            forge = viewModel.currentAddon.forgeVersion,
                            neoforge = viewModel.currentAddon.neoforgeVersion,
                            fabric = viewModel.currentAddon.fabricVersion,
                            fabricAPI = viewModel.currentAddon.fabricAPIVersion,
                            quilt = viewModel.currentAddon.quiltVersion,
                            quiltAPI = viewModel.currentAddon.quiltAPIVersion
                        )
                    )
                }
            )

            Column(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .verticalScroll(state = rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                val yOffset1 by swapAnimateDpAsState(targetValue = (-40).dp, swapIn = isVisible)
                OptiFineList(
                    modifier = Modifier.offset { IntOffset(x = 0, y = yOffset1.roundToPx()) },
                    currentAddon = viewModel.currentAddon,
                    refreshIcon = { viewModel.refreshIcon() },
                    addonList = viewModel.addonList
                ) { viewModel.reloadOptiFine() }

                val yOffset2 by swapAnimateDpAsState(targetValue = (-40).dp, swapIn = isVisible, delayMillis = 50)
                ForgeList(
                    modifier = Modifier.offset { IntOffset(x = 0, y = yOffset2.roundToPx()) },
                    currentAddon = viewModel.currentAddon,
                    refreshIcon = { viewModel.refreshIcon() },
                    addonList = viewModel.addonList
                ) { viewModel.reloadForge() }

                val yOffset3 by swapAnimateDpAsState(targetValue = (-40).dp, swapIn = isVisible, delayMillis = 100)
                NeoForgeList(
                    modifier = Modifier.offset { IntOffset(x = 0, y = yOffset3.roundToPx()) },
                    currentAddon = viewModel.currentAddon,
                    refreshIcon = { viewModel.refreshIcon() },
                    addonList = viewModel.addonList
                ) { viewModel.reloadNeoForge() }

                val yOffset4 by swapAnimateDpAsState(targetValue = (-40).dp, swapIn = isVisible, delayMillis = 150)
                FabricList(
                    modifier = Modifier.offset { IntOffset(x = 0, y = yOffset4.roundToPx()) },
                    currentAddon = viewModel.currentAddon,
                    refreshIcon = { viewModel.refreshIcon() },
                    addonList = viewModel.addonList
                ) { viewModel.reloadFabric() }

                val yOffset5 by swapAnimateDpAsState(targetValue = (-40).dp, swapIn = isVisible, delayMillis = 200)
                FabricAPIList(
                    modifier = Modifier.offset { IntOffset(x = 0, y = yOffset5.roundToPx()) },
                    currentAddon = viewModel.currentAddon,
                    refreshIcon = { viewModel.refreshIcon() },
                    addonList = viewModel.addonList
                ) { viewModel.reloadFabricAPI() }

                val yOffset6 by swapAnimateDpAsState(targetValue = (-40).dp, swapIn = isVisible, delayMillis = 250)
                QuiltList(
                    modifier = Modifier.offset { IntOffset(x = 0, y = yOffset6.roundToPx()) },
                    currentAddon = viewModel.currentAddon,
                    refreshIcon = { viewModel.refreshIcon() },
                    addonList = viewModel.addonList
                ) { viewModel.reloadQuilt() }

                val yOffset7 by swapAnimateDpAsState(targetValue = (-40).dp, swapIn = isVisible, delayMillis = 300)
                QuiltAPIList(
                    modifier = Modifier.offset { IntOffset(x = 0, y = yOffset7.roundToPx()) },
                    currentAddon = viewModel.currentAddon,
                    refreshIcon = { viewModel.refreshIcon() },
                    addonList = viewModel.addonList
                ) { viewModel.reloadQuiltAPI() }
            }
        }
    }
}

@Composable
private fun ScreenHeader(
    modifier: Modifier = Modifier,
    itemContainerColor: Color,
    itemContentColor: Color,
    gameVersion: String,
    currentAddon: CurrentAddon,
    refreshIcon: Any? = null,
    onInstall: (String) -> Unit = {}
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(modifier = Modifier.width(8.dp))

            VersionIconPreview(
                modifier = Modifier.size(28.dp),
                currentAddon = currentAddon,
                refreshIcon = refreshIcon
            )

            var nameValue by remember { mutableStateOf(gameVersion) }
            //用户是否对版本名称进行过编辑
            var editedByUser by remember { mutableStateOf(false) }

            AutoChangeVersionName(
                gameVersion = gameVersion,
                currentAddon = currentAddon,
                editedByUser = editedByUser,
                changeValue = {
                    nameValue = it
                }
            )

            var errorMessage by remember { mutableStateOf("") }

            val isError = nameValue.isEmpty().also {
                errorMessage = stringResource(R.string.generic_cannot_empty)
            } || isFilenameInvalid(nameValue) { message ->
                errorMessage = message
            } || VersionsManager.validateVersionName(nameValue, null) { message ->
                errorMessage = message
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp)
                    .animateContentSize(animationSpec = getAnimateTween())
            ) {
                SimpleTextInputField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = 4.dp),
                    value = nameValue,
                    onValueChange = {
                        nameValue = it
                        if (!editedByUser) {
                            //用户已经对版本名称进行了编辑
                            editedByUser = true
                        }
                    },
                    color = itemContainerColor,
                    contentColor = itemContentColor,
                    singleLine = true,
                    hint = {
                        Text(
                            text = stringResource(R.string.download_game_version_name),
                            style = TextStyle(color = LocalContentColor.current).copy(fontSize = 12.sp)
                        )
                    }
                )

                if (isError) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            IconButton(
                onClick = {
                    if (!isError) {
                        onInstall(nameValue)
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = stringResource(R.string.download_install)
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun VersionIconPreview(
    modifier: Modifier = Modifier,
    currentAddon: CurrentAddon,
    refreshIcon: Any? = null
) {
    val iconRes = remember(refreshIcon) {
        when {
            currentAddon.optifineVersion != null && currentAddon.forgeVersion != null -> R.drawable.img_anvil //OptiFine & Forge 同时选择
            currentAddon.optifineVersion != null -> R.drawable.img_loader_optifine
            currentAddon.forgeVersion != null -> R.drawable.img_anvil
            currentAddon.neoforgeVersion != null -> R.drawable.img_loader_neoforge
            currentAddon.fabricVersion != null -> R.drawable.img_loader_fabric
            currentAddon.quiltVersion != null -> R.drawable.img_loader_quilt
            else -> R.drawable.img_minecraft
        }
    }

    Image(
        modifier = modifier,
        painter = painterResource(id = iconRes),
        contentDescription = null
    )
}

/**
 * 根据当前已选择的Addon，自动修改版本名称
 * @param editedByUser 版本名称是否已被用户修改，如果用户已经修改过版本名称，则阻止自动修改
 */
@Composable
private fun AutoChangeVersionName(
    gameVersion: String,
    currentAddon: CurrentAddon,
    editedByUser: Boolean,
    changeValue: (String) -> Unit = {}
) {
    fun getOptiFine(optifine: OptiFineVersion) = "${ModLoader.OPTIFINE.displayName} ${optifine.realVersion}"
    fun getForge(forge: ForgeVersion) = "${ModLoader.FORGE.displayName} ${forge.versionName}"

    LaunchedEffect(
        currentAddon.optifineVersion,
        currentAddon.forgeVersion,
        currentAddon.neoforgeVersion,
        currentAddon.fabricVersion,
        currentAddon.quiltVersion
    ) {
        if (editedByUser) return@LaunchedEffect //用户已修改，阻止自动更改

        val modloaderValue = when {
            currentAddon.optifineVersion != null && currentAddon.forgeVersion != null -> {
                //OptiFine & Forge 同时选择
                val forge = getForge(currentAddon.forgeVersion!!)
                val optifine = getOptiFine(currentAddon.optifineVersion!!)
                "$forge-$optifine"
            }
            currentAddon.optifineVersion != null -> getOptiFine(currentAddon.optifineVersion!!)
            currentAddon.forgeVersion != null -> getForge(currentAddon.forgeVersion!!)
            currentAddon.neoforgeVersion != null -> "${ModLoader.NEOFORGE.displayName} ${currentAddon.neoforgeVersion!!.versionName}"
            currentAddon.fabricVersion != null -> "${ModLoader.FABRIC.displayName} ${currentAddon.fabricVersion!!.version}"
            currentAddon.quiltVersion != null -> "${ModLoader.QUILT.displayName} ${currentAddon.quiltVersion!!.version}"
            else -> null
        }

        changeValue(modloaderValue?.let { "$gameVersion $it" } ?: gameVersion)
    }
}

@Composable
private fun OptiFineList(
    modifier: Modifier = Modifier,
    currentAddon: CurrentAddon,
    addonList: AddonList,
    refreshIcon: () -> Unit,
    onReload: () -> Unit = {}
) {
    val items = remember(addonList.optifineList, currentAddon.forgeVersion) {
        addonList.optifineList?.filter { version ->
            currentAddon.forgeVersion?.let { forgeVersion ->
                version.forgeVersion?.let { required ->
                    required.isEmpty() || forgeVersion.forgeBuildVersion.compareOptiFineRequired(required)
                } ?: false
            } ?: true
        }
    }

    AddonListLayout(
        modifier = modifier,
        state = currentAddon.optifineState,
        title = ModLoader.OPTIFINE.displayName,
        iconPainter = painterResource(R.drawable.img_loader_optifine),
        items = items,
        current = currentAddon.optifineVersion,
        incompatibleSet = currentAddon.incompatibleWithOptiFine,
        checkIncompatible = {
            val ofType = listOf(ModLoader.OPTIFINE)
            currentAddon.optifineVersion?.let { version ->
                val forgeVersion = currentAddon.forgeVersion
                //检查与 Forge 的兼容性
                if (forgeVersion != null) {
                    if (isOptiFineCompatibleWithForge(version, forgeVersion)) {
                        currentAddon.incompatibleWithForge -= ofType
                    } else {
                        currentAddon.incompatibleWithForge += ofType
                        currentAddon.forgeVersion = null
                    }
                } else {
                    if (isOptiFineCompatibleWithForgeList(version, addonList.forgeList)) {
                        currentAddon.incompatibleWithForge -= ofType
                    } else {
                        currentAddon.incompatibleWithForge += ofType
                        currentAddon.forgeVersion = null
                    }
                }
                currentAddon.neoforgeVersion = null
                currentAddon.fabricVersion = null
                currentAddon.quiltVersion = null
                currentAddon.incompatibleWithNeoForge += ofType
                currentAddon.incompatibleWithFabric += ofType
                currentAddon.incompatibleWithFabricAPI += ofType
                currentAddon.incompatibleWithQuilt += ofType
                currentAddon.incompatibleWithQuiltAPI += ofType
            } ?: run {
                currentAddon.incompatibleWithForge -= ofType
                currentAddon.incompatibleWithNeoForge -= ofType
                currentAddon.incompatibleWithFabric -= ofType
                currentAddon.incompatibleWithFabricAPI -= ofType
                currentAddon.incompatibleWithQuilt -= ofType
                currentAddon.incompatibleWithQuiltAPI -= ofType
            }
        },
        triggerCheckIncompatible = arrayOf(currentAddon.forgeState),
        getItemText = { it.displayName },
        summary = { OptiFineVersionSummary(it) },
        onValueChange = { version ->
            currentAddon.optifineVersion = version
            refreshIcon()
        },
        onReload = onReload
    )
}

@Composable
private fun ForgeList(
    modifier: Modifier = Modifier,
    currentAddon: CurrentAddon,
    addonList: AddonList,
    refreshIcon: () -> Unit,
    onReload: () -> Unit = {}
) {
    val items = addonList.forgeList?.filter {
        //选择 OptiFine 之后，根据 OptiFine 需求的 Forge 版本进行过滤
        currentAddon.optifineVersion?.let { optifineVersion ->
            val requiredForgeVersion = optifineVersion.forgeVersion ?: return@filter false //null：不兼容任何Forge
            when {
                requiredForgeVersion.isEmpty() -> true //字符串为空：兼容所有
                else -> it.forgeBuildVersion.compareOptiFineRequired(requiredForgeVersion)
            }
        } ?: true
    }

    AddonListLayout(
        modifier = modifier,
        state = currentAddon.forgeState,
        title = ModLoader.FORGE.displayName,
        iconPainter = painterResource(R.drawable.img_anvil),
        items = items,
        current = currentAddon.forgeVersion,
        incompatibleSet = currentAddon.incompatibleWithForge,
        checkIncompatible = {
            val forgeType = listOf(ModLoader.FORGE)
            currentAddon.forgeVersion?.let { version ->
                val optiFineVersion = currentAddon.optifineVersion
                //检查与 OptiFine 的兼容性
                if (optiFineVersion != null) {
                    if (isOptiFineCompatibleWithForge(optiFineVersion, version)) {
                        currentAddon.incompatibleWithOptiFine -= forgeType
                    } else {
                        currentAddon.incompatibleWithOptiFine += forgeType
                        currentAddon.optifineVersion = null
                    }
                } else {
                    if (isForgeCompatibleWithOptiFineList(version, addonList.optifineList)) {
                        currentAddon.incompatibleWithForge -= forgeType
                    } else {
                        currentAddon.incompatibleWithOptiFine += forgeType
                        currentAddon.optifineVersion = null
                    }
                }
                currentAddon.neoforgeVersion = null
                currentAddon.fabricVersion = null
                currentAddon.quiltVersion = null
                currentAddon.incompatibleWithNeoForge += forgeType
                currentAddon.incompatibleWithFabric += forgeType
                currentAddon.incompatibleWithFabricAPI += forgeType
                currentAddon.incompatibleWithQuilt += forgeType
                currentAddon.incompatibleWithQuiltAPI += forgeType
            } ?: run {
                currentAddon.incompatibleWithOptiFine -= forgeType
                currentAddon.incompatibleWithNeoForge -= forgeType
                currentAddon.incompatibleWithFabric -= forgeType
                currentAddon.incompatibleWithFabricAPI -= forgeType
                currentAddon.incompatibleWithQuilt -= forgeType
                currentAddon.incompatibleWithQuiltAPI -= forgeType
            }
        },
        triggerCheckIncompatible = arrayOf(currentAddon.optifineState),
        error = checkForgeCompatibilityError(addonList.forgeList),
        getItemText = { it.versionName },
        summary = { ForgeVersionSummary(it) },
        onValueChange = { version ->
            currentAddon.forgeVersion = version
            refreshIcon()
        },
        onReload = onReload
    )
}

@Composable
private fun NeoForgeList(
    modifier: Modifier = Modifier,
    currentAddon: CurrentAddon,
    addonList: AddonList,
    refreshIcon: () -> Unit,
    onReload: () -> Unit = {}
) {
    AddonListLayout(
        modifier = modifier,
        state = currentAddon.neoforgeState,
        title = ModLoader.NEOFORGE.displayName,
        iconPainter = painterResource(R.drawable.img_loader_neoforge),
        items = addonList.neoforgeList,
        current = currentAddon.neoforgeVersion,
        incompatibleSet = currentAddon.incompatibleWithNeoForge,
        checkIncompatible = {
            val neoforgeType = listOf(ModLoader.NEOFORGE)
            currentAddon.neoforgeVersion?.let { version ->
                currentAddon.optifineVersion = null
                currentAddon.forgeVersion = null
                currentAddon.fabricVersion = null
                currentAddon.quiltVersion = null
                currentAddon.incompatibleWithOptiFine += neoforgeType
                currentAddon.incompatibleWithForge += neoforgeType
                currentAddon.incompatibleWithFabric += neoforgeType
                currentAddon.incompatibleWithFabricAPI += neoforgeType
                currentAddon.incompatibleWithQuilt += neoforgeType
                currentAddon.incompatibleWithQuiltAPI += neoforgeType
            } ?: run {
                currentAddon.incompatibleWithOptiFine -= neoforgeType
                currentAddon.incompatibleWithForge -= neoforgeType
                currentAddon.incompatibleWithFabric -= neoforgeType
                currentAddon.incompatibleWithFabricAPI -= neoforgeType
                currentAddon.incompatibleWithQuilt -= neoforgeType
                currentAddon.incompatibleWithQuiltAPI -= neoforgeType
            }
        },
        getItemText = { it.versionName },
        summary = { NeoForgeSummary(it) },
        onValueChange = { version ->
            currentAddon.neoforgeVersion = version
            refreshIcon()
        },
        onReload = onReload
    )
}

@Composable
private fun FabricList(
    modifier: Modifier = Modifier,
    currentAddon: CurrentAddon,
    addonList: AddonList,
    refreshIcon: () -> Unit,
    onReload: () -> Unit = {}
) {
    AddonListLayout(
        modifier = modifier,
        state = currentAddon.fabricState,
        title = ModLoader.FABRIC.displayName,
        iconPainter = painterResource(R.drawable.img_loader_fabric),
        items = addonList.fabricList,
        current = currentAddon.fabricVersion,
        incompatibleSet = currentAddon.incompatibleWithFabric,
        checkIncompatible = {
            val fabricType = listOf(ModLoader.FABRIC)
            currentAddon.fabricVersion?.let { version ->
                currentAddon.optifineVersion = null
                currentAddon.forgeVersion = null
                currentAddon.neoforgeVersion = null
                currentAddon.quiltVersion = null
                currentAddon.incompatibleWithOptiFine += fabricType
                currentAddon.incompatibleWithForge += fabricType
                currentAddon.incompatibleWithNeoForge += fabricType
                currentAddon.incompatibleWithQuilt += fabricType
                currentAddon.incompatibleWithQuiltAPI += fabricType
            } ?: run {
                currentAddon.incompatibleWithOptiFine -= fabricType
                currentAddon.incompatibleWithForge -= fabricType
                currentAddon.incompatibleWithNeoForge -= fabricType
                currentAddon.incompatibleWithQuilt -= fabricType
                currentAddon.incompatibleWithQuiltAPI -= fabricType
            }
        },
        getItemText = { it.version },
        summary = { FabricLikeSummary(it) },
        onValueChange = { version ->
            currentAddon.fabricVersion = version
            refreshIcon()
        },
        onReload = onReload
    )
}

@Composable
private fun FabricAPIList(
    modifier: Modifier = Modifier,
    currentAddon: CurrentAddon,
    requestString: String = stringResource(R.string.download_game_addon_request_addon, ModLoader.FABRIC.displayName),
    addonList: AddonList,
    refreshIcon: () -> Unit,
    onReload: () -> Unit = {}
) {
    val unSelectedFabric = remember(currentAddon.fabricVersion) {
        when {
            currentAddon.fabricVersion == null -> {
                currentAddon.fabricAPIVersion = null
                requestString
            }
            else -> null
        }
    }

    AddonListLayout(
        modifier = modifier,
        state = currentAddon.fabricAPIState,
        title = ModLoader.FABRIC_API.displayName,
        iconPainter = painterResource(R.drawable.img_loader_fabric),
        items = addonList.fabricAPIList,
        current = currentAddon.fabricAPIVersion,
        incompatibleSet = currentAddon.incompatibleWithFabricAPI,
        checkIncompatible = {
            val fabricType = listOf(ModLoader.FABRIC_API)
            currentAddon.fabricAPIVersion?.let { version ->
                currentAddon.optifineVersion = null
                currentAddon.forgeVersion = null
                currentAddon.neoforgeVersion = null
                currentAddon.quiltVersion = null
                currentAddon.incompatibleWithOptiFine += fabricType
                currentAddon.incompatibleWithForge += fabricType
                currentAddon.incompatibleWithNeoForge += fabricType
                currentAddon.incompatibleWithQuilt += fabricType
                currentAddon.incompatibleWithQuiltAPI += fabricType
            } ?: run {
                currentAddon.incompatibleWithOptiFine -= fabricType
                currentAddon.incompatibleWithForge -= fabricType
                currentAddon.incompatibleWithNeoForge -= fabricType
                currentAddon.incompatibleWithQuilt -= fabricType
                currentAddon.incompatibleWithQuiltAPI -= fabricType
            }
        },
        error = unSelectedFabric,
        getItemText = { it.displayName },
        summary = { ModSummary(it) },
        onValueChange = { version ->
            currentAddon.fabricAPIVersion = version
            refreshIcon()
        },
        onReload = onReload
    )
}

@Composable
private fun QuiltList(
    modifier: Modifier = Modifier,
    currentAddon: CurrentAddon,
    addonList: AddonList,
    refreshIcon: () -> Unit,
    onReload: () -> Unit = {}
) {
    AddonListLayout(
        modifier = modifier,
        state = currentAddon.quiltState,
        title = ModLoader.QUILT.displayName,
        iconPainter = painterResource(R.drawable.img_loader_quilt),
        items = addonList.quiltList,
        current = currentAddon.quiltVersion,
        incompatibleSet = currentAddon.incompatibleWithQuilt,
        checkIncompatible = {
            val quiltType = listOf(ModLoader.QUILT)
            currentAddon.quiltVersion?.let { version ->
                currentAddon.optifineVersion = null
                currentAddon.forgeVersion = null
                currentAddon.neoforgeVersion = null
                currentAddon.fabricVersion = null
                currentAddon.incompatibleWithOptiFine += quiltType
                currentAddon.incompatibleWithForge += quiltType
                currentAddon.incompatibleWithNeoForge += quiltType
                currentAddon.incompatibleWithFabric += quiltType
                currentAddon.incompatibleWithFabricAPI += quiltType
            } ?: run {
                currentAddon.incompatibleWithOptiFine -= quiltType
                currentAddon.incompatibleWithForge -= quiltType
                currentAddon.incompatibleWithNeoForge -= quiltType
                currentAddon.incompatibleWithFabric -= quiltType
                currentAddon.incompatibleWithFabricAPI -= quiltType
            }
        },
        getItemText = { it.version },
        summary = { FabricLikeSummary(it) },
        onValueChange =  { version ->
            currentAddon.quiltVersion = version
            refreshIcon()
        },
        onReload = onReload
    )
}

@Composable
private fun QuiltAPIList(
    modifier: Modifier = Modifier,
    currentAddon: CurrentAddon,
    requestString: String = stringResource(R.string.download_game_addon_request_addon, ModLoader.QUILT.displayName),
    addonList: AddonList,
    refreshIcon: () -> Unit,
    onReload: () -> Unit = {}
) {
    val unSelectedQuilt = remember(currentAddon.quiltVersion) {
        when {
            currentAddon.quiltVersion == null -> {
                currentAddon.quiltAPIVersion = null
                requestString
            }
            else -> null
        }
    }

    AddonListLayout(
        modifier = modifier,
        state = currentAddon.quiltAPIState,
        title = ModLoader.QUILT_API.displayName,
        iconPainter = painterResource(R.drawable.img_loader_quilt),
        items = addonList.quiltAPIList,
        current = currentAddon.quiltAPIVersion,
        incompatibleSet = currentAddon.incompatibleWithQuiltAPI,
        checkIncompatible = {
            val quiltType = listOf(ModLoader.QUILT_API)
            currentAddon.quiltAPIVersion?.let { version ->
                currentAddon.optifineVersion = null
                currentAddon.forgeVersion = null
                currentAddon.neoforgeVersion = null
                currentAddon.fabricVersion = null
                currentAddon.incompatibleWithOptiFine += quiltType
                currentAddon.incompatibleWithForge += quiltType
                currentAddon.incompatibleWithNeoForge += quiltType
                currentAddon.incompatibleWithFabric += quiltType
                currentAddon.incompatibleWithFabricAPI += quiltType
            } ?: run {
                currentAddon.incompatibleWithOptiFine -= quiltType
                currentAddon.incompatibleWithForge -= quiltType
                currentAddon.incompatibleWithNeoForge -= quiltType
                currentAddon.incompatibleWithFabric -= quiltType
                currentAddon.incompatibleWithFabricAPI -= quiltType
            }
        },
        error = unSelectedQuilt,
        getItemText = { it.displayName },
        summary = { ModSummary(it) },
        onValueChange =  { version ->
            currentAddon.quiltAPIVersion = version
            refreshIcon()
        },
        onReload = onReload
    )
}

private fun isOptiFineCompatibleWithForge(
    optifine: OptiFineVersion,
    forge: ForgeVersion
): Boolean = optifine.forgeVersion?.let {
    //空字符串表示兼容所有
    it.isEmpty() || forge.forgeBuildVersion.compareOptiFineRequired(it)
} ?: false //没有声明需要的 Forge 版本，视为不兼容

private fun isOptiFineCompatibleWithForgeList(
    optifine: OptiFineVersion,
    forgeList: List<ForgeVersion>?
): Boolean {
                                    //没有声明需要的 Forge 版本，视为不兼容
    val requiredVersion = optifine.forgeVersion ?: return false
    return when {
        requiredVersion.isEmpty() -> true //为空则表示不要求，兼容
        else -> forgeList?.any {
            it.forgeBuildVersion.compareOptiFineRequired(requiredVersion)
        } == true
    }
}

private fun isForgeCompatibleWithOptiFineList(
    forge: ForgeVersion,
    optifineList: List<OptiFineVersion>?
): Boolean {
    val forgeVersion = forge.forgeBuildVersion

    optifineList?.forEach { optifine ->
        val ofVersion = optifine.forgeVersion ?: return@forEach //null: 不兼容，跳过
        if (ofVersion.isEmpty()) return true    //空字符串表示兼容所有
        if (forgeVersion.compareOptiFineRequired(ofVersion)) return true
    }

    return false //没有匹配项
}

@Composable
private fun checkForgeCompatibilityError(
    forgeList: List<ForgeVersion>?
): String? {
    return when {
        forgeList == null -> null //保持默认的“不可用”
        forgeList.any { forgeVersion -> forgeVersion.category == "universal" || forgeVersion.category == "client" } -> {
            //跳过无法自动安装的版本
            stringResource(R.string.download_game_addon_not_installable)
        }
        else -> null
    }
}