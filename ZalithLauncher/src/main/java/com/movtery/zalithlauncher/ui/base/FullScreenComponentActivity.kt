package com.movtery.zalithlauncher.ui.base

import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.view.View
import android.view.View.OnSystemUiVisibilityChangeListener
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.annotation.CallSuper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.movtery.zalithlauncher.viewmodel.LauncherFullScreenViewModel
import kotlinx.coroutines.launch

abstract class FullScreenComponentActivity(
    protected val shouldIgnoreNotch: Boolean
) : AbstractComponentActivity() {
    val fullScreenViewModel: LauncherFullScreenViewModel by viewModels()

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFullscreen()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                fullScreenViewModel.refreshEvent.collect {
                    ignoreNotch()
                }
            }
        }
    }

    @CallSuper
    override fun onPostResume() {
        super.onPostResume()
        setFullscreen()
        ignoreNotch()
    }

    private fun setFullscreen() {
        val decorView = window.decorView
        val visibilityChangeListener =
            OnSystemUiVisibilityChangeListener { visibility: Int ->
                val multiWindowMode = isInMultiWindowMode
                // When in multi-window mode, asking for fullscreen makes no sense (cause the launcher runs in a window)
                // So, ignore the fullscreen setting when activity is in multi window mode
                if (!multiWindowMode) {
                    if ((visibility and View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                        decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                or View.SYSTEM_UI_FLAG_FULLSCREEN
                                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
                    }
                } else {
                    decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                }
            }
        decorView.setOnSystemUiVisibilityChangeListener(visibilityChangeListener)
        visibilityChangeListener.onSystemUiVisibilityChange(decorView.systemUiVisibility)
    }

    private fun ignoreNotch() {
        if (Build.VERSION.SDK_INT >= VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                if (shouldIgnoreNotch) {
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                } else {
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER
                }
            window.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            )
        }
    }
}