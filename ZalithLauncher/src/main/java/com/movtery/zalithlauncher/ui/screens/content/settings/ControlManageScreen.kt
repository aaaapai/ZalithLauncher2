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
import com.movtery.zalithlauncher.state.MutableStates
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.screens.content.SETTINGS_SCREEN_TAG
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SettingsBackground
import com.movtery.zalithlauncher.utils.animation.swapAnimateDpAsState

const val CONTROL_MANAGE_SCREEN_TAG = "ControlManageScreen"

@Composable
fun ControlManageScreen() {
    BaseScreen(
        parentScreenTag = SETTINGS_SCREEN_TAG,
        parentCurrentTag = MutableStates.mainScreenTag,
        childScreenTag = CONTROL_MANAGE_SCREEN_TAG,
        childCurrentTag = MutableStates.settingsScreenTag
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