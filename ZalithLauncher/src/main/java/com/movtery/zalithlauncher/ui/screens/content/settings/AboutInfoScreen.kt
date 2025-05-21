package com.movtery.zalithlauncher.ui.screens.content.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.state.MutableStates
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.itemLayoutColor
import com.movtery.zalithlauncher.ui.screens.content.SETTINGS_SCREEN_TAG
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SettingsBackground
import com.movtery.zalithlauncher.utils.animation.swapAnimateDpAsState
import com.movtery.zalithlauncher.utils.network.NetWorkUtils

const val ABOUT_INFO_SCREEN_TAG = "AboutInfoScreen"

private const val COPYRIGHT_AOSP = "Copyright © The Android Open Source Project"
private const val COPYRIGHT_KTOR = "Copyright © 2000-2023 JetBrains s.r.o."
private const val LICENSE_APACHE_2 = "Apache License 2.0"
private const val LICENSE_MIT = "MIT License"
private const val LICENSE_LGPL_3 = "LGPL-3.0 License"
private const val URL_KTOR = "https://ktor.io"

private val libraryData = listOf(
    LibraryInfo("androidx-constraintlayout-compose", COPYRIGHT_AOSP, LICENSE_APACHE_2, "https://developer.android.com/develop/ui/compose/layouts/constraintlayout"),
    LibraryInfo("androidx-material-icons-core", COPYRIGHT_AOSP, LICENSE_APACHE_2, "https://developer.android.com/jetpack/androidx/releases/compose-material"),
    LibraryInfo("androidx-material-icons-extended", COPYRIGHT_AOSP, LICENSE_APACHE_2, "https://developer.android.com/jetpack/androidx/releases/compose-material"),
    LibraryInfo("androidx-navigation-compose", COPYRIGHT_AOSP, LICENSE_APACHE_2, "https://developer.android.com/jetpack/compose/navigation"),
    LibraryInfo("Apache Commons Codec", null, LICENSE_APACHE_2, "https://commons.apache.org/proper/commons-codec"),
    LibraryInfo("Apache Commons Compress", null, LICENSE_APACHE_2, "https://commons.apache.org/proper/commons-compress"),
    LibraryInfo("Apache Commons IO", null, LICENSE_APACHE_2, "https://commons.apache.org/proper/commons-io"),
    LibraryInfo("ByteHook", "Copyright © 2020-2024 ByteDance, Inc.", LICENSE_MIT, "https://github.com/bytedance/bhook"),
    LibraryInfo("Coil Compose", "Copyright © 2025 Coil Contributors", LICENSE_APACHE_2, "https://github.com/coil-kt/coil"),
    LibraryInfo("Coil Gifs", "Copyright © 2025 Coil Contributors", LICENSE_APACHE_2, "https://github.com/coil-kt/coil"),
    LibraryInfo("colorpicker-compose", "Copyright © 2022 skydoves (Jaewoong Eum)", LICENSE_APACHE_2, "https://github.com/skydoves/colorpicker-compose"),
    LibraryInfo("Gson", "Copyright © 2008 Google Inc.", LICENSE_APACHE_2, "https://github.com/google/gson"),
    LibraryInfo("kotlinx.coroutines", "Copyright © 2000-2020 JetBrains s.r.o.", LICENSE_APACHE_2, "https://github.com/Kotlin/kotlinx.coroutines"),
    LibraryInfo("ktor-client-cio", COPYRIGHT_KTOR, LICENSE_APACHE_2, URL_KTOR),
    LibraryInfo("ktor-client-content-negotiation", COPYRIGHT_KTOR, LICENSE_APACHE_2, URL_KTOR),
    LibraryInfo("ktor-client-core", COPYRIGHT_KTOR, LICENSE_APACHE_2, URL_KTOR),
    LibraryInfo("ktor-http", COPYRIGHT_KTOR, LICENSE_APACHE_2, URL_KTOR),
    LibraryInfo("ktor-serialization-kotlinx-json", COPYRIGHT_KTOR, LICENSE_APACHE_2, URL_KTOR),
    LibraryInfo("material-color-utilities", "Copyright 2021 Google LLC", LICENSE_APACHE_2, "https://github.com/material-foundation/material-color-utilities"),
    LibraryInfo("Maven Artifact", "Copyright © The Apache Software Foundation", LICENSE_APACHE_2, "https://github.com/apache/maven/tree/maven-3.9.9/maven-artifact"),
    LibraryInfo("OkHttp", "Copyright © 2019 Square, Inc.", LICENSE_APACHE_2, "https://github.com/square/okhttp"),
    LibraryInfo("proxy-client-android", null, LICENSE_LGPL_3, "https://github.com/TouchController/TouchController"),
    LibraryInfo("StringFog", "Copyright © 2016-2023, Megatron King", LICENSE_APACHE_2, "https://github.com/MegatronKing/StringFog"),
    LibraryInfo("XZ for Java", "Copyright © The XZ for Java authors and contributors", "0BSD License", "https://tukaani.org/xz/java.html")
)

private data class LibraryInfo(
    val name: String,
    val copyrightInfo: String?,
    val license: String,
    val webUrl: String
)

@Composable
fun AboutInfoScreen() {
    BaseScreen(
        parentScreenTag = SETTINGS_SCREEN_TAG,
        parentCurrentTag = MutableStates.mainScreenTag,
        childScreenTag = ABOUT_INFO_SCREEN_TAG,
        childCurrentTag = MutableStates.settingsScreenTag
    ) { isVisible ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(state = rememberScrollState())
                .padding(all = 12.dp)
        ) {
            val yOffset by swapAnimateDpAsState(
                targetValue = (-40).dp,
                swapIn = isVisible
            )

            //额外依赖库板块
            SettingsBackground(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = 0,
                            y = yOffset.roundToPx()
                        )
                    },
                contentPadding = 0.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        text = stringResource(R.string.about_library_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(width = 8.dp))
                    HorizontalDivider(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .fillMaxWidth(),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(all = 12.dp)
                    ) {
                        repeat(libraryData.size) { index ->
                            val info = libraryData[index]
                            LibraryInfoItem(
                                info = info,
                                modifier = Modifier
                                    .padding(bottom = if (index == libraryData.size - 1) 0.dp else 12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryInfoItem(
    info: LibraryInfo,
    modifier: Modifier = Modifier,
    color: Color = itemLayoutColor(),
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Surface(
        modifier = modifier,
        color = color,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.large,
        shadowElevation = 1.dp,
        onClick = {}
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = info.name,
                    style = MaterialTheme.typography.titleMedium
                )
                info.copyrightInfo?.let { copyrightInfo ->
                    Text(
                        text = copyrightInfo,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    text = "Licensed under the ${info.license}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            val context = LocalContext.current
            IconButton(
                modifier = Modifier.align(Alignment.CenterVertically),
                onClick = {
                    NetWorkUtils.openLink(context, info.webUrl)
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Launch,
                    contentDescription = null
                )
            }
        }
    }
}