package com.movtery.zalithlauncher.ui.screens.content.elements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.ui.components.SimpleEditDialog
import com.movtery.zalithlauncher.utils.file.InvalidFilenameException
import com.movtery.zalithlauncher.utils.file.checkFilenameValidity
import com.movtery.zalithlauncher.utils.file.formatFileSize
import com.movtery.zalithlauncher.utils.formatDate
import org.apache.commons.io.FileUtils
import java.io.File
import java.util.Date

@Composable
fun BaseFileItem(
    file: File,
    modifier: Modifier = Modifier
) {
    if (!file.exists()) throw IllegalArgumentException("File is not exists!")

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            modifier = Modifier.size(24.dp).align(Alignment.CenterVertically),
            imageVector = if (file.isDirectory) Icons.Outlined.Folder else Icons.Outlined.Description,
            contentDescription = null
        )
        Column(modifier = Modifier.align(Alignment.CenterVertically)) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.labelMedium
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val date = Date(file.lastModified())
                Text(
                    modifier = Modifier.alpha(0.7f),
                    text = formatDate(
                        date = date,
                        pattern = stringResource(R.string.date_format)
                    ),
                    style = MaterialTheme.typography.labelSmall
                )
                if (file.isFile) {
                    Text(
                        text = formatFileSize(FileUtils.sizeOf(file)),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
fun CreateNewDirDialog(
    onDismissRequest: () -> Unit = {},
    createDir: (name: String) -> Unit = {}
) {
    var value by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    val isError = value.isEmpty() || isFilenameInvalid(value) { message ->
        errorMessage = message
    }

    SimpleEditDialog(
        title = stringResource(R.string.files_create_dir),
        value = value,
        onValueChange = { value = it },
        isError = isError,
        supportingText = {
            when {
                value.isEmpty() -> Text(text = stringResource(R.string.generic_cannot_empty))
                isError -> Text(text = errorMessage)
            }
        },
        singleLine = true,
        onDismissRequest = onDismissRequest,
        onConfirm = { if (!isError) createDir(value) }
    )
}

@Composable
fun isFilenameInvalid(
    str: String,
    onError: (message: String) -> Unit
): Boolean {
    return try {
        checkFilenameValidity(str)
        false
    } catch (e: InvalidFilenameException) {
        onError(
            when {
                e.containsIllegalCharacters() -> stringResource(R.string.generic_input_invalid_character, e.illegalCharacters)
                e.isInvalidLength -> stringResource(R.string.file_invalid_length, e.invalidLength, 255)
                else -> ""
            }
        )
        true
    }
}