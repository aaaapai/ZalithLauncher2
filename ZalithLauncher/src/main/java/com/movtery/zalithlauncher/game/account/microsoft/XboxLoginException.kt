package com.movtery.zalithlauncher.game.account.microsoft

import android.content.Context
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.account.microsoft.XboxLoginException.ExceptionStatus.BANNED
import com.movtery.zalithlauncher.game.account.microsoft.XboxLoginException.ExceptionStatus.BLOCKED_REGION
import com.movtery.zalithlauncher.game.account.microsoft.XboxLoginException.ExceptionStatus.NOT_ACCEPTED_SERVICE
import com.movtery.zalithlauncher.game.account.microsoft.XboxLoginException.ExceptionStatus.REACHED_PLAYTIME_LIMIT
import com.movtery.zalithlauncher.game.account.microsoft.XboxLoginException.ExceptionStatus.REQUIRES_PROOF_OF_AGE
import com.movtery.zalithlauncher.game.account.microsoft.XboxLoginException.ExceptionStatus.RESTRICTED
import com.movtery.zalithlauncher.game.account.microsoft.XboxLoginException.ExceptionStatus.UNDERAGE
import com.movtery.zalithlauncher.game.account.microsoft.XboxLoginException.ExceptionStatus.UNREGISTERED

/**
 * Xbox 登陆出现的各种异常
 */
class XboxLoginException(val status: ExceptionStatus) : RuntimeException() {
    enum class ExceptionStatus {
        /**
         * 被封禁
         */
        BANNED,

        /**
         * 受到限制
         */
        RESTRICTED,

        /**
         * 未创建个人资料
         */
        UNREGISTERED,

        /**
         * 未同意服务条款
         */
        NOT_ACCEPTED_SERVICE,

        /**
         * 地区被禁止登陆
         */
        BLOCKED_REGION,

        /**
         * 未提供年龄证明
         */
        REQUIRES_PROOF_OF_AGE,

        /**
         * 达到游玩时间限制
         */
        REACHED_PLAYTIME_LIMIT,

        /**
         * 未成年
         */
        UNDERAGE
    }
}

fun XboxLoginException.toLocal(context: Context): String {
    return when(status) {
        BANNED -> context.getString(R.string.account_logging_xbox_banned)
        RESTRICTED -> context.getString(R.string.account_logging_xbox_restricted)
        UNREGISTERED -> context.getString(R.string.account_logging_xbox_unregistered)
        NOT_ACCEPTED_SERVICE -> context.getString(R.string.account_logging_xbox_not_accepted_service)
        BLOCKED_REGION -> context.getString(R.string.account_logging_xbox_blocked_region)
        REQUIRES_PROOF_OF_AGE -> context.getString(R.string.account_logging_xbox_requires_proof_of_age)
        REACHED_PLAYTIME_LIMIT -> context.getString(R.string.account_logging_xbox_reached_playtime_limit)
        UNDERAGE -> context.getString(R.string.account_logging_xbox_underage)
    }
}