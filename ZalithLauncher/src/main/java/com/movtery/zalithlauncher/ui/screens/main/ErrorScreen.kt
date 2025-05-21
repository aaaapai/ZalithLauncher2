package com.movtery.zalithlauncher.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.info.InfoDistributor
import com.movtery.zalithlauncher.ui.components.ScalingActionButton

@Composable
fun ErrorScreen(
    message: String,
    messageBody: String,
    canRestart: Boolean = true,
    onRestartClick: () -> Unit = {},
    onExitClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopBar(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .zIndex(10f),
            color = MaterialTheme.colorScheme.surfaceContainer
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = MaterialTheme.colorScheme.surface)
            ) {
                ErrorContent(
                    modifier = Modifier
                        .weight(7f)
                        .padding(start = 12.dp, top = 12.dp, bottom = 12.dp),
                    message = message,
                    messageBody = messageBody,

                )

                ActionContext(
                    modifier = Modifier
                        .weight(3f)
                        .padding(all = 12.dp),
                    canRestart = canRestart,
                    onRestartClick = onRestartClick,
                    onExitClick = onExitClick
                )
            }
        }
    }
}

@Composable
private fun TopBar(
    modifier: Modifier = Modifier,
    color: Color
) {
    Surface(
        modifier = modifier,
        color = color,
        shadowElevation = 3.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                text = stringResource(R.string.crash_launcher_title, InfoDistributor.LAUNCHER_NAME)
            )
        }
    }
}

@Composable
private fun ErrorContent(
    modifier: Modifier = Modifier,
    message: String,
    messageBody: String
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(state = rememberScrollState())
                .padding(all = 12.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = messageBody,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ActionContext(
    modifier: Modifier = Modifier,
    canRestart: Boolean,
    onRestartClick: () -> Unit = {},
    onExitClick: () -> Unit = {}
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxSize(),
            verticalArrangement = Arrangement.Bottom
        ) {
            if (canRestart) {
                ScalingActionButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onRestartClick
                ) {
                    Text(text = stringResource(R.string.crash_restart))
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
            ScalingActionButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onExitClick
            ) {
                Text(text = stringResource(R.string.crash_exit))
            }
        }
    }
}