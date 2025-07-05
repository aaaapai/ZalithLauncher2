package com.movtery.zalithlauncher.ui.screens.content.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.screens.content.SettingsScreenKey
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SettingsBackground
import com.movtery.zalithlauncher.ui.screens.content.settingsScreenKey
import com.movtery.zalithlauncher.ui.screens.main.elements.mainScreenKey
import com.movtery.zalithlauncher.utils.animation.swapAnimateDpAsState
import kotlinx.serialization.Serializable

@Serializable
data object ControlManageScreenKey: NavKey

@Composable
fun ControlManageScreen() {
    BaseScreen(
        Triple(SettingsScreenKey, mainScreenKey, false),
        Triple(ControlManageScreenKey, settingsScreenKey, false)
    ) { isVisible ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(all = 12.dp)
        ) {
            val yOffset by swapAnimateDpAsState(
                targetValue = (-40).dp,
                swapIn = isVisible
            )

            SettingsBackground(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = 0,
                            y = yOffset.roundToPx()
                        )
                    }
            ) {
                Box(modifier = Modifier.fillMaxSize()) { Text(modifier = Modifier.align(Alignment.Center) , text = "TODO") }
            }
        }
    }
}