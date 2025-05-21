package com.movtery.zalithlauncher.ui.screens.content.settings.layouts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SettingsBackground(
    modifier: Modifier = Modifier,
    content: @Composable SettingsLayoutScope.() -> Unit
) {
    SettingsBackground(
        modifier = modifier,
        contentPadding = 8.dp,
        content = content
    )
}

@Composable
fun SettingsBackground(
    modifier: Modifier = Modifier,
    contentPadding: Dp,
    content: @Composable SettingsLayoutScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier
                .clip(shape = MaterialTheme.shapes.large)
                .padding(contentPadding)
        ) {
            val scope = remember { SettingsLayoutScope() }

            with(scope) {
                content()
            }
        }
    }
}