package com.movtery.zalithlauncher.ui.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.movtery.zalithlauncher.SplashException
import com.movtery.zalithlauncher.components.Components
import com.movtery.zalithlauncher.components.InstallableItem
import com.movtery.zalithlauncher.components.UnpackComponentsTask
import com.movtery.zalithlauncher.components.jre.Jre
import com.movtery.zalithlauncher.components.jre.UnpackJnaTask
import com.movtery.zalithlauncher.components.jre.UnpackJreTask
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.ui.base.BaseComponentActivity
import com.movtery.zalithlauncher.ui.screens.splash.SplashScreen
import com.movtery.zalithlauncher.ui.theme.ZalithLauncherTheme
import com.movtery.zalithlauncher.utils.logging.Logger.lInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseComponentActivity(refreshData = false) {
    private val unpackItems: MutableList<InstallableItem> = ArrayList()
    private var finishedTaskCount = AtomicInteger(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()

        initUnpackItems()
        checkAllTask()

        setContent {
            if (checkTasksToMain()) {
                return@setContent
            }

            ZalithLauncherTheme {
                Box {
                    SplashScreen(
                        startAllTask = { startAllTask() },
                        unpackItems = unpackItems
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

    private fun initUnpackItems() {
        Components.entries.forEach { component ->
            val task = UnpackComponentsTask(this@SplashActivity, component)
            if (!task.isCheckFailed()) {
                unpackItems.add(
                    InstallableItem(
                        component.displayName,
                        getString(component.summary),
                        task
                    )
                )
            }
        }
        Jre.entries.forEach { jre ->
            val task = UnpackJreTask(this@SplashActivity, jre)
            if (!task.isCheckFailed()) {
                unpackItems.add(
                    InstallableItem(
                        jre.jreName,
                        getString(jre.summary),
                        task
                    )
                )
            }
        }
        val jnaTask = UnpackJnaTask(this@SplashActivity)
        if (!jnaTask.isCheckFailed()) {
            unpackItems.add(
                InstallableItem(
                    "JNA",
                    null,
                    jnaTask
                )
            )
        }
        unpackItems.sort()
    }

    private fun checkAllTask() {
        unpackItems.forEach { item ->
            if (!item.task.isNeedUnpack()) {
                item.isFinished = true
                finishedTaskCount.incrementAndGet()
            }
        }
    }

    private fun startAllTask() {
        lifecycleScope.launch {
            val jobs = unpackItems
                .filter { !it.isFinished }
                .map { item ->
                    launch(Dispatchers.IO) {
                        item.isRunning = true
                        runCatching {
                            item.task.run()
                        }.onFailure {
                            throw SplashException(it)
                        }
                        finishedTaskCount.incrementAndGet()
                        item.isRunning = false
                        item.isFinished = true
                    }
                }
            jobs.joinAll()
        }.invokeOnCompletion {
            AllSettings.javaRuntime.apply {
                //检查并设置默认的Java环境
                if (getValue().isEmpty()) save(Jre.JRE_8.jreName)
            }
            swapToMain()
        }
    }

    private fun checkTasksToMain(): Boolean {
        val toMain = finishedTaskCount.get() >= unpackItems.size
        if (toMain) {
            lInfo("All content that needs to be extracted is already the latest version!")
            swapToMain()
        }
        return toMain
    }

    private fun swapToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}