package com.movtery.zalithlauncher.ui.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.ui.activities.FatalErrorActivity.Companion.BUNDLE_THROWABLE
import com.movtery.zalithlauncher.ui.base.AbstractComponentActivity
import com.movtery.zalithlauncher.utils.copyText
import com.movtery.zalithlauncher.utils.getSerializableSafely

/**
 * 用于显示致命崩溃信息的 Activity
 *
 * 此 Activity 会向用户展示一个 AlertDialog，详细说明崩溃情况
 *
 * 它被设计为与主启动器相互独立，以确保即使启动器本身出现严重问题，也能正常显示该界面
 */
class FatalErrorActivity : AbstractComponentActivity() {

    companion object {
        const val BUNDLE_THROWABLE = "BUNDLE_THROWABLE"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val extras = intent.extras ?: return run { finish() }
        val throwable = extras.getSerializableSafely(BUNDLE_THROWABLE, Throwable::class.java) ?: return run { finish() }

        val message = getString(R.string.crash_launch_crash_message)
        val throwableStack = Log.getStackTraceString(throwable)

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.crash_launch_crash_title))
            .setMessage(message + "\n\n" + throwableStack)
            .setPositiveButton(android.R.string.ok) { _, _ -> finish() }
            .setNeutralButton(android.R.string.copy) { _, _ ->
                copyText(null, throwableStack, this)
                finish()
            }
            .setCancelable(false)
            .show()
    }
}

/**
 * 使用指定的 throwable 显示致命错误界面
 */
fun showFatalError(context: Context, throwable: Throwable) {
    val intent = Intent(context, FatalErrorActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        putExtra(BUNDLE_THROWABLE, throwable)
    }
    context.startActivity(intent)
}