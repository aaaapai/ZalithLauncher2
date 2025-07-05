package com.movtery.zalithlauncher.game.account

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.movtery.zalithlauncher.game.skin.SkinFileDownloader
import com.movtery.zalithlauncher.game.skin.SkinModelType
import com.movtery.zalithlauncher.game.skin.getLocalUUIDWithSkinModel
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.utils.logging.Logger.lError
import com.movtery.zalithlauncher.utils.logging.Logger.lInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import java.io.File
import java.util.UUID

@Entity(tableName = "accounts")
data class Account(
    /**
     * 唯一 UUID，标识该账号
     */
    @PrimaryKey
    val uniqueUUID: String = UUID.randomUUID().toString().lowercase(),
    var accessToken: String = "0",
    var clientToken: String = "0",
    var username: String = "Steve",
    var profileId: String = getLocalUUIDWithSkinModel(username, SkinModelType.NONE),
    var refreshToken: String = "0",
    var xUid: String? = null,
    var otherBaseUrl: String? = null,
    var otherAccount: String? = null,
    var otherPassword: String? = null,
    var accountType: String? = null,
    var skinModelType: SkinModelType = SkinModelType.NONE
) {
    val hasSkinFile: Boolean
        get() = getSkinFile().exists()

    fun getSkinFile() = File(PathManager.DIR_ACCOUNT_SKIN, "$uniqueUUID.png")

    /**
     * 下载并更新账号的皮肤文件
     */
    suspend fun downloadSkin() = withContext(Dispatchers.IO) {
        when {
            isMicrosoftAccount() -> updateSkin("https://sessionserver.mojang.com")
            isAuthServerAccount() -> updateSkin(otherBaseUrl!!.removeSuffix("/") + "/sessionserver/")
            else -> {}
        }
    }

    private suspend fun updateSkin(url: String) {
        val skinFile = getSkinFile()
        if (skinFile.exists()) FileUtils.deleteQuietly(skinFile) //清除一次皮肤文件

        runCatching {
            SkinFileDownloader().yggdrasil(url, skinFile, profileId)
            lInfo("Update skin success")
        }.onFailure { e ->
            lError("Could not update skin", e)
        }
    }
}
