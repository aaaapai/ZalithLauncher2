package com.movtery.zalithlauncher.ui.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.launch.LogName
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.ui.activities.ErrorActivity.Companion.BUNDLE_CAN_RESTART
import com.movtery.zalithlauncher.ui.activities.ErrorActivity.Companion.BUNDLE_EXIT_TYPE
import com.movtery.zalithlauncher.ui.activities.ErrorActivity.Companion.BUNDLE_THROWABLE
import com.movtery.zalithlauncher.ui.activities.ErrorActivity.Companion.EXIT_LAUNCHER
import com.movtery.zalithlauncher.ui.base.BaseComponentActivity
import com.movtery.zalithlauncher.ui.screens.main.ErrorScreen
import com.movtery.zalithlauncher.ui.theme.ZalithLauncherTheme
import com.movtery.zalithlauncher.utils.file.shareFile
import com.movtery.zalithlauncher.utils.getInt
import com.movtery.zalithlauncher.utils.getParcelableSafely
import com.movtery.zalithlauncher.utils.getSerializableSafely
import com.movtery.zalithlauncher.utils.string.StringUtils
import com.movtery.zalithlauncher.utils.toBoolean
import java.io.File

class ErrorActivity : BaseComponentActivity(refreshData = false) {

    companion object {
        const val BUNDLE_EXIT_TYPE = "BUNDLE_EXIT_TYPE"
        const val BUNDLE_THROWABLE = "BUNDLE_THROWABLE"
        const val BUNDLE_JVM_CRASH = "BUNDLE_JVM_CRASH"
        const val BUNDLE_CAN_RESTART = "BUNDLE_CAN_RESTART"
        const val EXIT_JVM = "EXIT_JVM"
        const val EXIT_LAUNCHER = "EXIT_LAUNCHER"

        @JvmStatic
        fun showExitMessage(context: Context, code: Int, isSignal: Boolean) {
            val intent = Intent(context, ErrorActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(BUNDLE_EXIT_TYPE, EXIT_JVM)
                putExtra(BUNDLE_JVM_CRASH, JvmCrash(code, isSignal))
            }
            context.startActivity(intent)
        }

        private data class JvmCrash(val code: Int, val isSignal: Boolean) : Parcelable {
            constructor(parcel: Parcel) : this(
                parcel.readInt(),
                parcel.readInt().toBoolean()
            )

            override fun describeContents(): Int = 0

            override fun writeToParcel(dest: Parcel, flags: Int) {
                dest.writeInt(code)
                dest.writeInt(isSignal.getInt())
            }

            companion object CREATOR : Parcelable.Creator<JvmCrash> {
                override fun createFromParcel(parcel: Parcel): JvmCrash {
                    return JvmCrash(parcel)
                }

                override fun newArray(size: Int): Array<JvmCrash?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val extras = intent.extras ?: return runFinish()

        val exitType = extras.getString(BUNDLE_EXIT_TYPE, EXIT_LAUNCHER)

        val errorMessage = when (exitType) {
            EXIT_JVM -> {
                val jvmCrash = extras.getParcelableSafely(BUNDLE_JVM_CRASH, JvmCrash::class.java) ?: return runFinish()
                val messageResId = if (jvmCrash.isSignal) R.string.crash_singnal_message else R.string.crash_exit_message
                val message = getString(messageResId, jvmCrash.code)
                val messageBody = getString(R.string.crash_exit_note)
                ErrorMessage(
                    message = message,
                    messageBody = messageBody,
                    crashType = CrashType.GAME_CRASH
                )
            }
            else -> {
                val throwable = extras.getSerializableSafely(BUNDLE_THROWABLE, Throwable::class.java) ?: return runFinish()
                val message = getString(R.string.crash_launcher_message)
                val messageBody = StringUtils.throwableToString(throwable)
                ErrorMessage(
                    message = message,
                    messageBody = messageBody,
                    crashType = CrashType.LAUNCHER_CRASH
                )
            }
        }

        val logFile = when (exitType) {
            EXIT_JVM -> {
                File(PathManager.DIR_FILES_EXTERNAL, "${LogName.GAME.fileName}.log")
            }
            else -> {
                PathManager.FILE_CRASH_REPORT
            }
        }

        val canRestart: Boolean = extras.getBoolean(BUNDLE_CAN_RESTART, true)

        setContent {
            ZalithLauncherTheme {
                Box {
                    ErrorScreen(
                        crashType = errorMessage.crashType,
                        message = errorMessage.message,
                        messageBody = errorMessage.messageBody,
                        shareLogs = logFile.exists() && logFile.isFile,
                        canRestart = canRestart,
                        onShareLogsClick = {
                            if (logFile.exists() && logFile.isFile) {
                                shareFile(this@ErrorActivity, logFile)
                            }
                        },
                        onRestartClick = {
                            startActivity(Intent(this@ErrorActivity, MainActivity::class.java))
                            finish()
                        },
                        onExitClick = { finish() }
                    )
                    LauncherVersion(
                        modifier = Modifier
                            .padding(vertical = 2.dp)
                            .align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }

    private data class ErrorMessage(
        val message: String,
        val messageBody: String,
        val crashType: CrashType
    )
}

/**
 * 崩溃类型
 */
enum class CrashType(val textRes: Int) {
    /**
     * 启动器崩溃
     */
    LAUNCHER_CRASH(R.string.crash_type_launcher),

    /**
     * 游戏运行崩溃
     */
    GAME_CRASH(R.string.crash_type_game)
}

/**
 * 启动软件崩溃信息页面
 */
fun showLauncherCrash(context: Context, throwable: Throwable, canRestart: Boolean = true) {
    val intent = Intent(context, ErrorActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        putExtra(BUNDLE_EXIT_TYPE, EXIT_LAUNCHER)
        putExtra(BUNDLE_THROWABLE, throwable)
        putExtra(BUNDLE_CAN_RESTART, canRestart)
    }
    context.startActivity(intent)
}