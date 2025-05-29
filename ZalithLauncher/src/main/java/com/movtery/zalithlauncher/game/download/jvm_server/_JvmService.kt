package com.movtery.zalithlauncher.game.download.jvm_server

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle

const val PROCESS_SERVICE_PORT = 53151 //random

//构造变量
const val SERVICE_JVM_ARGS = "service.jvm.args"
const val SERVICE_JRE_NAME = "service.jre.name"
const val SERVICE_USER_HOME = "service.user.home"

fun startJvmService(
    context: Context,
    jvmArgs: String,
    jreName: String? = null,
    userHome: String? = null
) {
    val bundle = Bundle().apply {
        putString(SERVICE_JVM_ARGS, jvmArgs)
        putString(SERVICE_JRE_NAME, jreName)
        putString(SERVICE_USER_HOME, userHome)
    }
    val intent = Intent(context, JvmService::class.java).apply {
        putExtras(bundle)
    }
    context.startForegroundService(intent)
}

/**
 * 当前是否只有主进程正在运行
 */
fun isOnlyMainProcessesRunning(context: Context): Boolean {
    return (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).runningAppProcesses.size == 1
}