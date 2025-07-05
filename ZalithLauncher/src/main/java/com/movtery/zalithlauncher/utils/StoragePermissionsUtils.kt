package com.movtery.zalithlauncher.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.movtery.zalithlauncher.R

class StoragePermissionsUtils {
    companion object {
        private const val REQUEST_CODE_PERMISSIONS: Int = 0
        @JvmStatic
        private var hasStoragePermission: Boolean = false

        /**
         * 检查存储权限，返回是否拥有存储权限
         */
        @JvmStatic
        fun checkPermissionsForInit(context: Context) {
            hasStoragePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                checkPermissionsForAndroid11AndAbove()
            } else {
                hasStoragePermissions(context)
            }
        }

        /**
         * 获得提前检查好的存储权限
         */
        @JvmStatic
        fun checkPermissions() = hasStoragePermission

        /**
         * 检查存储权限，如果没有存储权限，则弹出弹窗向用户申请
         */
        @JvmStatic
        fun checkPermissions(
            activity: Activity,
            title: Int = R.string.generic_warning,
            message: String,
            messageSdk30: String = message,
            hasPermission: () -> Unit = {},
            onDialogCancel: () -> Unit = {}
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                handlePermissionsForAndroid11AndAbove(activity, title, messageSdk30, hasPermission, onDialogCancel)
            } else {
                handlePermissionsForAndroid10AndBelow(activity, title, message, hasPermission, onDialogCancel)
            }
        }

        /**
         * 适用于安卓10及一下的存储权限检查
         */
        fun hasStoragePermissions(context: Context): Boolean {
            return ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }

        @RequiresApi(api = Build.VERSION_CODES.R)
        private fun checkPermissionsForAndroid11AndAbove() = Environment.isExternalStorageManager()

        @RequiresApi(api = Build.VERSION_CODES.R)
        private fun handlePermissionsForAndroid11AndAbove(
            activity: Activity,
            title: Int,
            message: String,
            hasPermission: () -> Unit = {},
            onDialogCancel: () -> Unit = {}
        ) {
            if (!checkPermissionsForAndroid11AndAbove()) {
                showPermissionRequestDialog(activity, title, message, object : RequestPermissions {
                    override fun onRequest() {
                        val intent =
                            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                        intent.data = ("package:${activity.packageName}").toUri()
                        activity.startActivityForResult(intent, REQUEST_CODE_PERMISSIONS)
                    }

                    override fun onCancel() {
                        onDialogCancel()
                    }
                })
            } else {
                hasPermission()
            }
        }

        private fun handlePermissionsForAndroid10AndBelow(
            activity: Activity,
            title: Int,
            message: String,
            hasPermission: () -> Unit = {},
            onDialogCancel: () -> Unit = {}
        ) {
            if (!hasStoragePermissions(activity)) {
                showPermissionRequestDialog(activity, title, message, object : RequestPermissions {
                    override fun onRequest() {
                        ActivityCompat.requestPermissions(
                            activity, arrayOf(
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                            ), REQUEST_CODE_PERMISSIONS
                        )
                    }

                    override fun onCancel() {
                        onDialogCancel()
                    }
                })
            } else {
                hasPermission()
            }
        }

        private fun showPermissionRequestDialog(
            context: Context,
            title: Int,
            message: String,
            requestPermissions: RequestPermissions
        ) {
            MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.generic_confirm) { _, _ -> requestPermissions.onRequest() }
                .setNegativeButton(R.string.generic_cancel) { _, _ -> requestPermissions.onCancel() }
                .setCancelable(false)
                .show()
        }
    }

    private interface RequestPermissions {
        fun onRequest()
        fun onCancel()
    }
}