package com.movtery.zalithlauncher.ui.screens.content.versions.elements

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.download.assets.utils.getMcmodTitle
import com.movtery.zalithlauncher.game.version.mod.LocalMod
import com.movtery.zalithlauncher.game.version.mod.RemoteMod
import com.movtery.zalithlauncher.ui.components.ProgressDialog
import com.movtery.zalithlauncher.ui.components.SimpleAlertDialog

sealed interface ModsOperation {
    data object None : ModsOperation
    /** 执行任务中 */
    data object Progress : ModsOperation
    /** 删除模组对话框 */
    data class Delete(val mod: LocalMod) : ModsOperation
}

@Composable
fun ModsOperation(
    modsOperation: ModsOperation,
    updateOperation: (ModsOperation) -> Unit,
    onDelete: (LocalMod) -> Unit
) {
    when (modsOperation) {
        is ModsOperation.None -> {}
        is ModsOperation.Progress -> {
            ProgressDialog()
        }
        is ModsOperation.Delete -> {
            val mod = modsOperation.mod
            SimpleAlertDialog(
                title = stringResource(R.string.generic_warning),
                text = stringResource(R.string.mods_manage_delete_warning, mod.name),
                onDismiss = {
                    updateOperation(ModsOperation.None)
                },
                onConfirm = {
                    onDelete(mod)
                    updateOperation(ModsOperation.None)
                }
            )
        }
    }
}

/**
 * 根据名称，筛选模组
 */
fun List<RemoteMod>.filterMods(
    nameFilter: String
) = this.filter { mod ->
    nameFilter.isEmpty() || (
            mod.localMod.file.name.contains(nameFilter, true) ||
            mod.localMod.name.contains(nameFilter, true) ||
            mod.projectInfo?.title?.contains(nameFilter, true) == true ||
            mod.mcMod?.getMcmodTitle(mod.localMod.name)?.contains(nameFilter, true) == true
    )
}