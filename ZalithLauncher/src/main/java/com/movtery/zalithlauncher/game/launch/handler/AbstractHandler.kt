package com.movtery.zalithlauncher.game.launch.handler

import android.view.KeyEvent
import android.view.Surface
import androidx.annotation.CallSuper
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntSize
import com.movtery.zalithlauncher.game.launch.Launcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

abstract class AbstractHandler(
    val type: HandlerType,
    protected val getWindowSize: () -> IntSize,
    val launcher: Launcher,
    val onExit: (code: Int) -> Unit
) {
    var mIsSurfaceDestroyed: Boolean = false

    @CallSuper
    open suspend fun execute(
        surface: Surface?,
        scope: CoroutineScope
    ) {
        scope.launch(Dispatchers.Default) {
            val code = launcher.launch()
            onExit(code)
        }
    }

    abstract fun onPause()
    abstract fun onResume()
    abstract fun onGraphicOutput()
    abstract fun shouldIgnoreKeyEvent(event: KeyEvent): Boolean

    @Composable
    abstract fun getComposableLayout(): @Composable () -> Unit
}