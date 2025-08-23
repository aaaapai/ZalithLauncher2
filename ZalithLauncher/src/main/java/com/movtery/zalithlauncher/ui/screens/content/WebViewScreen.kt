package com.movtery.zalithlauncher.ui.screens.content

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation3.runtime.NavBackStack
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.navigateTo
import com.movtery.zalithlauncher.viewmodel.ScreenBackStackViewModel
import org.apache.commons.io.FileUtils

/**
 * 导航至WebViewScreen并访问特定网址
 */
fun NavBackStack.navigateToWeb(webUrl: String) = this.navigateTo(
    screenKey = NormalNavKey.WebScreen(webUrl),
    useClassEquality = true
)

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(
    key: NormalNavKey.WebScreen,
    backStackViewModel: ScreenBackStackViewModel
) {
    BaseScreen(
        screenKey = key,
        currentKey = backStackViewModel.mainScreenKey,
        useClassEquality = true
    ) { isVisible ->

        val context = LocalContext.current
        var isWebLoading by rememberSaveable { mutableStateOf(true) }

        val webViewHolder = remember {
            mutableStateOf<WebView?>(null)
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (isVisible) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = {
                        WebView(context).apply {
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isWebLoading = false
                                }

                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    isWebLoading = true
                                }
                            }

                            settings.javaScriptEnabled = true
                            settings.cacheMode = WebSettings.LOAD_NO_CACHE
                            loadUrl(key.url)
                            webViewHolder.value = this
                        }
                    },
                    update = {
                        //不在此处重复加载 url
                    }
                )
            } else {
                webViewHolder.value?.apply {
                    stopLoading()
                    loadUrl("about:blank")
                    clearHistory()
                    removeAllViews()
                    destroy()
                }
                webViewHolder.value = null

                val webCache = context.getDir("webview", 0)
                FileUtils.deleteQuietly(webCache)
                CookieManager.getInstance().removeAllCookies(null)
            }

            if (isWebLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp)
                        .align(Alignment.TopCenter)
                )
            }
        }
    }
}