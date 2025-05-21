package com.movtery.zalithlauncher.game.launch.handler

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.KeyEvent
import android.view.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntSize
import com.movtery.zalithlauncher.bridge.ZLBridge
import com.movtery.zalithlauncher.game.account.AccountsManager
import com.movtery.zalithlauncher.game.account.isLocalAccount
import com.movtery.zalithlauncher.game.input.EfficientAndroidLWJGLKeycode
import com.movtery.zalithlauncher.game.input.LWJGLCharSender
import com.movtery.zalithlauncher.game.keycodes.LwjglGlfwKeycode
import com.movtery.zalithlauncher.game.launch.GameLauncher
import com.movtery.zalithlauncher.game.launch.MCOptions
import com.movtery.zalithlauncher.game.launch.loadLanguage
import com.movtery.zalithlauncher.game.skin.SkinModelType
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.info.InfoDistributor
import com.movtery.zalithlauncher.ui.screens.game.GameScreen
import com.movtery.zalithlauncher.utils.file.child
import com.movtery.zalithlauncher.utils.file.ensureDirectory
import com.movtery.zalithlauncher.utils.file.zipDirRecursive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import org.lwjgl.glfw.CallbackBridge
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipOutputStream
import kotlin.io.path.createTempDirectory

class GameHandler(
    private val context: Context,
    private val version: Version,
    getWindowSize: () -> IntSize,
    gameLauncher: GameLauncher,
    onExit: (code: Int) -> Unit
) : AbstractHandler(HandlerType.GAME, getWindowSize, gameLauncher, onExit) {
    private val isTouchProxyEnabled = version.isTouchProxyEnabled()

    override suspend fun execute(surface: Surface?, scope: CoroutineScope) {
        ZLBridge.setupBridgeWindow(surface)

        MCOptions.setup(context, version)

        MCOptions.apply {
            set("fullscreen", "false")
            set("overrideWidth", CallbackBridge.windowWidth.toString())
            set("overrideHeight", CallbackBridge.windowHeight.toString())
            loadLanguage(version.getVersionInfo()!!.minecraftVersion)
            localSkinResourcePack()
            save()
        }

        super.execute(surface, scope)
    }

    override fun onPause() {
    }

    override fun onResume() {
    }

    override fun onGraphicOutput() {
    }

    @Suppress("DEPRECATION")
    override fun shouldIgnoreKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_UP && (event.flags and KeyEvent.FLAG_CANCELED) != 0) return false
        if ((event.flags and KeyEvent.FLAG_SOFT_KEYBOARD) == KeyEvent.FLAG_SOFT_KEYBOARD) {
            if (event.keyCode == KeyEvent.KEYCODE_ENTER) {
                LWJGLCharSender.sendEnter()
                return false
            }
        }

        EfficientAndroidLWJGLKeycode.getIndexByKey(event.keyCode).takeIf { it >= 0 }?.let { index ->
            EfficientAndroidLWJGLKeycode.execKey(event, index)
            return false
        }

        return when (event.keyCode) {
            KeyEvent.KEYCODE_UNKNOWN,
            KeyEvent.ACTION_MULTIPLE,
            KeyEvent.ACTION_UP
                 -> false

            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_UP
                 -> true

            else -> (event.flags and KeyEvent.FLAG_FALLBACK) != KeyEvent.FLAG_FALLBACK
        }
    }

    override fun sendMouseRight(isPressed: Boolean) {
        CallbackBridge.sendMouseButton(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_RIGHT.toInt(), isPressed)
    }

    @SuppressLint("ClickableViewAccessibility")
    @Composable
    override fun getComposableLayout() = @Composable {
        GameScreen(isTouchProxyEnabled, getWindowSize)
    }

    private suspend fun localSkinResourcePack() {
        AccountsManager.getCurrentAccount()?.takeIf {
            it.isLocalAccount() &&
            it.skinModelType != SkinModelType.NONE
        }?.let { account ->
            val modelType = SkinModelType.entries.find { it == account.skinModelType } ?: return@let

            version.getVersionInfo()!!.getMcVersionCode().takeIf { it.main !in 0..5 }?.let { versionCode ->
                val mainCode = versionCode.main
                val subCode = versionCode.sub

                /**
                 * [Reference PCL2](https://github.com/Hex-Dragon/PCL2/blob/dc611a982f8f97fab2c4275d1176db484f8549a4/Plain%20Craft%20Launcher%202/Modules/Minecraft/ModLaunch.vb#L1960-L1999)
                 */
                val packFormat = when (mainCode) {
                    in 6..8 -> 1
                    in 9..10 -> 2
                    in 11..12 -> 3
                    in 13..14 -> 4
                    15 -> 5
                    16 -> 6
                    17 -> 7
                    18 -> if (subCode <= 2) 8 else 9
                    19 -> if (subCode <= 3) 9 else 12
                    20 -> if (subCode <= 1) 15 else 17
                    else -> 17
                }

                val isOldType = when {
                    mainCode < 19 -> true
                    mainCode == 19 -> subCode <= 2
                    else -> false
                }

                tryPackSkinResourcePack(
                    packFormat,
                    isOldType = isOldType,
                    skinFile = account.getSkinFile(),
                    modelType = modelType
                )?.let { pack ->
                    val name = if (mainCode >= 13 || mainCode < 6) "file/${pack.name}" else pack.name
                    val resourcePacks = "resourcePacks"

                    MCOptions.set(
                        resourcePacks,
                        MCOptions.getAsList(resourcePacks).toMutableList().apply {
                            if (!contains(name)) {
                                if (!contains("vanilla")) {
                                    //顶层必须是原版，否则mc会直接抛弃所有资源包..？
                                    add(0, "vanilla")
                                }
                                val insertIndex = indexOfFirst { it == "vanilla" }
                                add(insertIndex + 1, name)
                            }
                        }
                    )
                }
            } ?: run {
                Log.w("GameHandler", "Version is too old to use the resource pack.")
            }
        }
    }

    /**
     * 尝试为离线账号打包一个皮肤资源包
     */
    private suspend fun tryPackSkinResourcePack(
        packFormat: Int,
        isOldType: Boolean,
        skinFile: File,
        modelType: SkinModelType
    ): File? = withContext(Dispatchers.IO) {
        runCatching {
            val resourcePackFile = File(
                File(version.getGameDir(), "resourcepacks").ensureDirectory(),
                "ZLSkin-pack.zip"
            )
            if (resourcePackFile.exists() && !resourcePackFile.delete()) throw IOException("Cannot clear an existing skin pack!")

            val packMcMetaContent = """{"pack":{"pack_format":${packFormat},"description":"${InfoDistributor.LAUNCHER_NAME} Offline Skin Resource Pack"}}""".trimIndent()

            val tempDir = createTempDirectory(prefix = "zlskin_pack_").toFile()
            try {
                val mcMetaFile = File(tempDir, "pack.mcmeta")
                mcMetaFile.writeText(packMcMetaContent)

                val entityBaseDir = tempDir.child("assets", "minecraft", "textures", "entity")

                val allTargets = if (isOldType) {
                    val targetFileName = when (modelType) {
                        SkinModelType.ALEX -> "alex.png"
                        SkinModelType.STEVE -> "steve.png"
                        SkinModelType.NONE -> error("It should not pass in SkinModelType.NONE.")
                    }
                    listOf(entityBaseDir.child(targetFileName))
                } else {
                    val skinBaseDir = entityBaseDir.child("player", modelType.string)
                    skinBaseDir.mkdirs()

                    //22w45a新增的皮肤类型
                    listOf("alex", "ari", "efe", "kai", "makena", "noor", "steve", "sunny", "zuri")
                        .map { File(skinBaseDir, "$it.png") }
                }

                allTargets.forEach { target ->
                    FileUtils.copyFile(skinFile, target)
                }

                ZipOutputStream(BufferedOutputStream(FileOutputStream(resourcePackFile))).use { zipOut ->
                    zipDirRecursive(tempDir, tempDir, zipOut)
                }

                resourcePackFile
            } finally {
                FileUtils.deleteDirectory(tempDir)
            }
        }.onFailure {
            Log.w("PackSkinResourcePack", "Failed to pack a skin resource pack!", it)
        }.getOrNull()
    }
}