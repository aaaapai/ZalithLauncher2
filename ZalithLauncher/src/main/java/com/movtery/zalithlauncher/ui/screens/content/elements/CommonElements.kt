package com.movtery.zalithlauncher.ui.screens.content.elements

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun CategoryIcon(iconRes: Int, textRes: Int, iconPadding: PaddingValues = PaddingValues()) {
    Icon(
        painter = painterResource(iconRes),
        contentDescription = stringResource(textRes),
        modifier = Modifier
            .size(24.dp)
            .padding(iconPadding)
    )
}

@Composable
fun CategoryIcon(image: ImageVector, textRes: Int) {
    Icon(
        imageVector = image,
        contentDescription = stringResource(textRes),
        modifier = Modifier.size(24.dp)
    )
}

data class CategoryItem(
    val tag: String,
    val icon: @Composable () -> Unit,
    val textRes: Int,
    val division: Boolean = false
)