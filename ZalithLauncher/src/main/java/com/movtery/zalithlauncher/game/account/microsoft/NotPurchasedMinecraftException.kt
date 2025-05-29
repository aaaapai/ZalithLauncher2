package com.movtery.zalithlauncher.game.account.microsoft

import android.content.Context
import com.movtery.zalithlauncher.R

class NotPurchasedMinecraftException : RuntimeException()

fun toLocal(context: Context): String {
    return context.getString(R.string.account_logging_not_purchased_minecraft)
}
