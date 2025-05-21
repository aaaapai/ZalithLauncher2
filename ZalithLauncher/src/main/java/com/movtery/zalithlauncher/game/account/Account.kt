package com.movtery.zalithlauncher.game.account

import android.util.Log
import com.movtery.zalithlauncher.game.skin.SkinFileDownloader
import com.movtery.zalithlauncher.game.skin.SkinModelType
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.utils.CryptoManager
import com.movtery.zalithlauncher.utils.GSON
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import java.io.File
import java.util.UUID

class Account(
    var accessToken: String = "0",
    var clientToken: String = "0",
    var profileId: String = "00000000-0000-0000-0000-000000000000",
    var username: String = "Steve",
    var refreshToken: String = "0",
    var xuid: String? = null,
    var otherBaseUrl: String? = null,
    var otherAccount: String? = null,
    var otherPassword: String? = null,
    var accountType: String? = null,
    var skinModelType: SkinModelType = SkinModelType.ALEX
) {
    /**
     * 唯一 UUID，标识该账号
     */
    val uniqueUUID: String = UUID.randomUUID().toString().lowercase()

    val hasSkinFile: Boolean
        get() = getSkinFile().exists()

    fun save() {
        val accountFile = File(PathManager.DIR_ACCOUNT, uniqueUUID)
        val rawJson = GSON.toJson(this)
        val encryptedData = CryptoManager.encrypt(rawJson)
        accountFile.writeText(encryptedData)
    }

    fun getSkinFile() = File(PathManager.DIR_ACCOUNT_SKIN, "$uniqueUUID.png")

    /**
     * 下载并更新账号的皮肤文件
     */
    suspend fun downloadSkin() = withContext(Dispatchers.IO) {
        when {
            isMicrosoftAccount() -> updateSkin("https://sessionserver.mojang.com")
            isOtherLoginAccount() -> updateSkin(otherBaseUrl!!.removeSuffix("/") + "/sessionserver/")
            else -> {}
        }
    }

    private suspend fun updateSkin(url: String) {
        val skinFile = getSkinFile()
        if (skinFile.exists()) FileUtils.deleteQuietly(skinFile) //清除一次皮肤文件

        runCatching {
            SkinFileDownloader().yggdrasil(url, skinFile, profileId)
            Log.i("Account", "Update skin success")
        }.onFailure { e ->
            Log.e("Account", "Could not update skin", e)
        }
    }
}