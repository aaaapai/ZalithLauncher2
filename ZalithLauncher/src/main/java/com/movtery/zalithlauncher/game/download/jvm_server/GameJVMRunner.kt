package com.movtery.zalithlauncher.game.download.jvm_server

import com.movtery.zalithlauncher.components.jre.Jre
import com.movtery.zalithlauncher.context.GlobalContext
import com.movtery.zalithlauncher.utils.logging.Logger.lInfo
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * 运行一个简易的JVM环境，安装ModLoader，同时在jvm退出时，尝试使用其他的Java环境重试
 * @param logId 记录日志的 tag
 * @param start 刚开始启动会调用的回调
 */
suspend fun runJvmRetryRuntimes(
    logId: String,
    jvmArgs: String,
    prefixArgs: (Jre) -> String?,
    jre: Jre,
    userHome: String,
    start: () -> Unit = {}
): Unit = withContext(Dispatchers.Default) {
    while (!isOnlyMainProcessesRunning(context = GlobalContext)) {
        lInfo("$logId Waiting for other processes stop...")
        delay(100)
    }

    start()

    val finalArgs = prefixArgs(jre)?.let {
        "$it $jvmArgs"
    } ?: jvmArgs

    val exitCode = startJvmServiceAndWaitExit(
        jvmArgs = finalArgs,
        jreName = jre.jreName,
        userHome = userHome
    )

    if (exitCode != 0) {
        val nextJava: Jre? = when (jre) {
            Jre.JRE_8 -> Jre.JRE_17
            Jre.JRE_17 -> Jre.JRE_21
            else -> null
        }

        nextJava?.let { jre ->
            lInfo("Retry with jre ${jre.name}...")
            runJvmRetryRuntimes(
                logId,
                jvmArgs,
                prefixArgs,
                jre,
                userHome
            )
        } ?: throw JvmCrashException(exitCode)
    }
}

suspend fun startJvmServiceAndWaitExit(
    jvmArgs: String,
    jreName: String? = null,
    userHome: String? = null
): Int = withContext(Dispatchers.IO) {
    val doneSignal = CompletableDeferred<Unit>()

    startJvmService(
        context = GlobalContext,
        jvmArgs = jvmArgs,
        userHome = userHome,
        jreName = jreName
    )

    JVMSocketServer.start { receiveMsg ->
        lInfo("receive msg: $receiveMsg, stopping server...")
        if (!doneSignal.isCompleted) {
            doneSignal.complete(Unit)
        }
        JVMSocketServer.stop()
    }
    doneSignal.await()

    val code = JVMSocketServer.receiveMsg?.toIntOrNull()
    lInfo("receive exit code: ${code ?: "unknown, default 0"}")
    code ?: 0
}