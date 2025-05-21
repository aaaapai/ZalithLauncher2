package com.movtery.zalithlauncher.game.launch.handler

import android.graphics.Canvas
import android.graphics.Paint
import android.util.Log
import android.view.KeyEvent
import android.view.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntSize
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withSave
import com.movtery.zalithlauncher.bridge.ZLBridge
import com.movtery.zalithlauncher.game.input.AWTInputEvent
import com.movtery.zalithlauncher.game.launch.JvmLauncher
import com.movtery.zalithlauncher.ui.screens.game.JVMScreen
import com.movtery.zalithlauncher.utils.string.StringUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class JVMHandler(
    jvmLauncher: JvmLauncher,
    getWindowSize: () -> IntSize,
    onExit: (code: Int) -> Unit
) : AbstractHandler(HandlerType.JVM, getWindowSize, jvmLauncher, onExit) {

    override suspend fun execute(surface: Surface?, scope: CoroutineScope) {
        surface?.run {
            val windowSize = getWindowSize()

            val canvasWidth = (windowSize.width * 0.8).toInt()
            val canvasHeight = (windowSize.height * 0.8).toInt()

            scope.launch(Dispatchers.Default) {
                var canvas: Canvas?
                val rgbArrayBitmap = createBitmap(canvasWidth, canvasHeight)
                val paint = Paint()

                try {
                    while (!mIsSurfaceDestroyed && surface.isValid) {
                        canvas = surface.lockCanvas(null)
                        canvas?.drawRGB(0, 0, 0)

                        ZLBridge.renderAWTScreenFrame()?.let { rgbArray ->
                            canvas?.withSave {
                                rgbArrayBitmap.setPixels(
                                    rgbArray,
                                    0,
                                    canvasWidth,
                                    0,
                                    0,
                                    canvasWidth,
                                    canvasHeight
                                )
                                this.drawBitmap(rgbArrayBitmap, 0f, 0f, paint)
                            }
                        }

                        canvas?.let { surface.unlockCanvasAndPost(it) }
                    }
                } catch (throwable: Throwable) {
                    Log.e("JVMHandler", StringUtils.throwableToString(throwable))
                } finally {
                    rgbArrayBitmap.recycle()
                    surface.release()
                }
            }
        }
        super.execute(surface, scope)
    }

    override fun onPause() {
    }

    override fun onResume() {
    }

    override fun onGraphicOutput() {
    }

    override fun shouldIgnoreKeyEvent(event: KeyEvent): Boolean {
        return true
    }

    override fun sendMouseRight(isPressed: Boolean) {
        ZLBridge.sendMousePress(AWTInputEvent.BUTTON3_DOWN_MASK, isPressed)
    }

    @Composable
    override fun getComposableLayout() = @Composable {
        JVMScreen()
    }
}