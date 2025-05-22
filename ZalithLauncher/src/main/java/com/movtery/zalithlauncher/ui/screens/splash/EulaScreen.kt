package com.movtery.zalithlauncher.ui.screens.splash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.state.MutableStates
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.ScalingActionButton
import com.movtery.zalithlauncher.utils.animation.swapAnimateDpAsState

const val EULA_SCREEN_TAG = "EulaScreen"

@Composable
fun EulaScreen(
    eulaText: String,
    onContinue: () -> Unit = {}
) {
    BaseScreen(
        screenTag = EULA_SCREEN_TAG,
        currentTag = MutableStates.splashScreenTag
    ) { isVisible ->
        Row(modifier = Modifier.fillMaxSize()) {
            EulaTextLayout(
                isVisible = isVisible,
                eulaText = eulaText,
                modifier = Modifier
                    .weight(7f)
                    .fillMaxHeight()
                    .padding(start = 12.dp, top = 12.dp, bottom = 12.dp)
            )
            ActionLayout(
                isVisible = isVisible,
                modifier = Modifier
                    .weight(3f)
                    .fillMaxHeight()
                    .padding(all = 12.dp),
                onContinue = onContinue
            )
        }
    }
}

@Composable
private fun EulaTextLayout(
    isVisible: Boolean,
    eulaText: String,
    modifier: Modifier = Modifier
) {
    val yOffset by swapAnimateDpAsState(
        targetValue = (-40).dp,
        swapIn = isVisible
    )

    Card(
        modifier = modifier
            .offset {
                IntOffset(
                    x = 0,
                    y = yOffset.roundToPx()
                )
            },
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = eulaText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun ActionLayout(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    onContinue: () -> Unit = {}
) {
    val xOffset by swapAnimateDpAsState(
        targetValue = 40.dp,
        swapIn = isVisible,
        isHorizontal = true
    )

    Card(
        modifier = modifier
            .offset {
                IntOffset(
                    x = xOffset.roundToPx(),
                    y = 0
                )
            },
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom
        ) {
            ScalingActionButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PaddingValues(horizontal = 12.dp, vertical = 8.dp)),
                onClick = onContinue
            ) {
                Text(text = stringResource(R.string.splash_screen_continue))
            }
        }
    }
}