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
import com.movtery.zalithlauncher.ui.activities.ErrorActivity.Companion.BUNDLE_CAN_RESTART
import com.movtery.zalithlauncher.ui.activities.ErrorActivity.Companion.BUNDLE_EXIT_TYPE
import com.movtery.zalithlauncher.ui.activities.ErrorActivity.Companion.BUNDLE_THROWABLE
import com.movtery.zalithlauncher.ui.activities.ErrorActivity.Companion.EXIT_LAUNCHER
import com.movtery.zalithlauncher.ui.base.BaseComponentActivity
import com.movtery.zalithlauncher.ui.screens.main.ErrorScreen
import com.movtery.zalithlauncher.ui.theme.ZalithLauncherTheme
import com.movtery.zalithlauncher.utils.getInt
import com.movtery.zalithlauncher.utils.getParcelableSafely
import com.movtery.zalithlauncher.utils.getSerializableSafely
import com.movtery.zalithlauncher.utils.string.StringUtils
import com.movtery.zalithlauncher.utils.toBoolean

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

        val (msg1, msg2) = when (exitType) {
            EXIT_JVM -> {
                val jvmCrash = extras.getParcelableSafely(BUNDLE_JVM_CRASH, JvmCrash::class.java) ?: return runFinish()
                val messageResId = if (jvmCrash.isSignal) R.string.crash_singnal_message else R.string.crash_exit_message
                val msg1 = getString(messageResId, jvmCrash.code)
                val msg2 = getString(R.string.crash_exit_note)
                msg1 to msg2
            }
            else -> {
                val throwable = extras.getSerializableSafely(BUNDLE_THROWABLE, Throwable::class.java) ?: return runFinish()
                val msg1 = getString(R.string.crash_launcher_message)
                val msg2 = StringUtils.throwableToString(throwable)
                msg1 to msg2
            }
        }

        val canRestart: Boolean = extras.getBoolean(BUNDLE_CAN_RESTART, true)

        setContent {
            ZalithLauncherTheme {
                Box {
                    ErrorScreen(
                        message = msg1,
                        messageBody = msg2,
                        canRestart = canRestart,
                        onRestartClick = {
                            startActivity(Intent(this@ErrorActivity, MainActivity::class.java))
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