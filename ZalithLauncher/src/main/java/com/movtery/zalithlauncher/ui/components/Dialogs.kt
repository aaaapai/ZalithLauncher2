package com.movtery.zalithlauncher.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.movtery.zalithlauncher.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * 展示警告对话框
 *
 * @param title 对话框标题
 * @param text 对话框内容
 * @param onConfirm 点击确认按钮的回调
 * @param onDismiss 点击取消或对话框外部的回调
 */
@Composable
fun SimpleAlertDialog(
    title: String,
    text: String,
    confirmText: String = stringResource(R.string.generic_confirm),
    dismissText: String = stringResource(R.string.generic_cancel),
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(text = text)
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(text = confirmText)
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(text = dismissText)
            }
        }
    )
}

/**
 * 展示警告对话框
 *
 * @param title 对话框标题
 * @param text 对话框内容
 * @param onDismiss 点击确认或对话框外部的回调
 */
@Composable
fun SimpleAlertDialog(
    title: String,
    text: String,
    confirmText: String = stringResource(R.string.generic_confirm),
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(text = text)
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(text = confirmText)
            }
        }
    )
}

@Composable
fun SimpleAlertDialog(
    title: String,
    text: @Composable () -> Unit = {},
    confirmText: String = stringResource(R.string.generic_confirm),
    dismissText: String = stringResource(R.string.generic_cancel),
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onDismissRequest: () -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                text()
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(text = confirmText)
            }
        },
        dismissButton = {
            Button(onClick = onCancel) {
                Text(text = dismissText)
            }
        }
    )
}

@Composable
fun SimpleEditDialog(
    title: String,
    value: String,
    onValueChange: (newValue: String) -> Unit,
    label: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false,
    maxLines: Int = 3,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    extraBody: @Composable (() -> Unit)? = null,
    extraContent: @Composable (() -> Unit)? = null,
    onDismissRequest: () -> Unit = {},
    onConfirm: () -> Unit = {},
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    extraBody?.let {
                        it.invoke()
                        Spacer(modifier = Modifier.size(8.dp))
                    }

                    val focusManager = LocalFocusManager.current

                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = value,
                        onValueChange = { onValueChange(it) },
                        label = label,
                        isError = isError,
                        supportingText = supportingText,
                        singleLine = singleLine,
                        maxLines = maxLines,
                        keyboardOptions = keyboardOptions.copy(
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus(true)
                                onConfirm()
                            }
                        ),
                        shape = MaterialTheme.shapes.large
                    )
                    extraContent?.invoke()
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = onDismissRequest
                    ) {
                        Text(text = stringResource(R.string.generic_cancel))
                    }
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = onConfirm
                    ) {
                        Text(text = stringResource(R.string.generic_confirm))
                    }
                }
            }
        }
    }
}

@Composable
fun SimpleCheckEditDialog(
    title: String,
    text: String,
    value: String,
    checked: Boolean,
    checkBoxText: String? = null,
    onValueChange: (newValue: String) -> Unit,
    onCheckedChange: (newValue: Boolean) -> Unit,
    label: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false,
    maxLines: Int = 3,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onDismissRequest: () -> Unit = {},
    onConfirm: () -> Unit = {},
) {
    SimpleEditDialog(
        title = title,
        value = value,
        onValueChange = onValueChange,
        label = label,
        isError = isError,
        supportingText = supportingText,
        singleLine = singleLine,
        maxLines = maxLines,
        keyboardOptions = keyboardOptions,
        extraBody = {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall
            )
        },
        extraContent = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    checkBoxText?.let{ Text(text = it, style = MaterialTheme.typography.labelMedium) }
                    Checkbox(
                        checked = checked,
                        onCheckedChange = onCheckedChange
                    )
                }
            }
        },
        onDismissRequest = onDismissRequest,
        onConfirm = onConfirm
    )
}

/**
 * 一个很简单的列表Dialog
 * @param itemsProvider 提供需要列出的items
 * @param itemTextProvider 提供单个item的展示文本
 * @param onItemSelected item被点击的回调
 * @param onDismissRequest dialog被关闭的回调
 * @param showConfirmAndCancel 是否通过确认和取消按钮来触发item的点击回调
 */
@Composable
fun <T> SimpleListDialog(
    title: String,
    itemsProvider: () -> List<T>,
    itemTextProvider: (T) -> String,
    onItemSelected: (T) -> Unit,
    onDismissRequest: () -> Unit,
    showConfirmAndCancel: Boolean = false
) {
    var selectedItem: T? by remember { mutableStateOf(null) }
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .wrapContentHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.size(16.dp))
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    items(itemsProvider()) { item ->
                        SimpleListItem(
                            selected = false,
                            itemName = itemTextProvider(item),
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                selectedItem = item
                                if (!showConfirmAndCancel) {
                                    onItemSelected(item)
                                    onDismissRequest()
                                }
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.size(4.dp))

                if (showConfirmAndCancel) {
                    Button(
                        onClick = {
                            if (selectedItem != null) {
                                onItemSelected(selectedItem!!)
                                onDismissRequest()
                            }
                        }
                    ) {
                        Text(stringResource(R.string.generic_confirm))
                    }
                }
            }
        }
    }
}

@Composable
fun SimpleTaskDialog(
    title: String,
    text: String? = null,
    task: suspend () -> Unit,
    context: CoroutineContext = Dispatchers.Default,
    onDismiss: () -> Unit,
    onError: (Throwable) -> Unit = {}
) {
    var inProgress by rememberSaveable { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    if (inProgress) {
        ProgressDialog(
            title = title,
            text = text
        )
    } else {
        onDismiss()
    }

    LaunchedEffect(Unit) {
        inProgress = true
        scope.launch(context) {
            try {
                task()
            } catch (e: Throwable) {
                onError(e)
            } finally {
                inProgress = false
            }
        }
    }
}

@Composable
fun ProgressDialog(
    title: String = stringResource(R.string.generic_in_progress),
    text: String? = null
) {
    Dialog(onDismissRequest = {}) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .wrapContentHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                text?.let {
                    Text(text = it, style = MaterialTheme.typography.labelSmall)
                }
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}