package com.movtery.zalithlauncher.ui.components

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.notification.NotificationManager

/**
 * 通知权限检查
 * @param onGranted 用户授予了权限
 * @param onIgnore 用户忽略了权限申请（拒绝）
 * @param onDismiss 用户关闭了权限申请弹窗
 */
@Composable
fun NotificationCheck(
    onGranted: () -> Unit = {},
    onIgnore: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    val context = LocalContext.current

    val requestPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                onGranted()
            } else {
                onIgnore()
            }
        }

    SimpleAlertDialog(
        title = stringResource(R.string.notification_title),
        text = {
            Text(text = stringResource(R.string.notification_data_jvm_service_message))
        },
        confirmText = stringResource(R.string.notification_request),
        dismissText = stringResource(R.string.notification_ignore),
        onConfirm = {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                //13- 跳转至设置，让用户自行开启通知权限
                NotificationManager.openNotificationSettings(context)
                onDismiss()
            } else {
                //安卓 13+ 可以直接弹出通知权限申请
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        onCancel = {
            onIgnore()
        },
        onDismissRequest = {
            onDismiss()
        }
    )
}