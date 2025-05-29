package com.movtery.zalithlauncher.game.account.microsoft

import android.content.Context
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.account.microsoft.MinecraftProfileException.ExceptionStatus.BLOCKED_IP
import com.movtery.zalithlauncher.game.account.microsoft.MinecraftProfileException.ExceptionStatus.FREQUENT
import com.movtery.zalithlauncher.game.account.microsoft.MinecraftProfileException.ExceptionStatus.PROFILE_NOT_EXISTS

/**
 * Minecraft 配置获取异常
 */
class MinecraftProfileException(val status: ExceptionStatus) : RuntimeException() {
    enum class ExceptionStatus {
        /**
         * 登陆过于频繁
         */
        FREQUENT,

        /**
         * IP 地址被禁止
         */
        BLOCKED_IP,

        /**
         * 未创建配置
         */
        PROFILE_NOT_EXISTS
    }
}

fun MinecraftProfileException.toLocal(context: Context): String {
    return when (status) {
        FREQUENT -> context.getString(R.string.account_logging_frequent)
        BLOCKED_IP -> context.getString(R.string.account_logging_blocked_ip)
        PROFILE_NOT_EXISTS -> context.getString(R.string.account_logging_profile_not_exists)
    }
}