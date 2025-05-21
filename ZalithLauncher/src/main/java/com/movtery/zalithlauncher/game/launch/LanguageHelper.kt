package com.movtery.zalithlauncher.game.launch

import com.movtery.zalithlauncher.game.version.installed.utils.isLowerOrEqualVer
import com.movtery.zalithlauncher.utils.getSystemLanguage

private fun getOlderLanguage(lang: String): String {
    val underscoreIndex = lang.indexOf('_')
    return if (underscoreIndex != -1) {
        //只将下划线后面的字符转换为大写
        val builder = StringBuilder(lang.substring(0, underscoreIndex + 1))
        builder.append(lang.substring(underscoreIndex + 1).uppercase())
        builder.toString()
    } else lang
}

private fun getLanguage(versionId: String): String {
    val lang = getSystemLanguage()
    return if (versionId.isLowerOrEqualVer("1.10.2", "16w32a")) {
        getOlderLanguage(lang) // 1.10 -
    } else {
        lang
    }
}

fun MCOptions.loadLanguage(versionId: String) {
    if (!containsKey("lang")) {
        val lang = getLanguage(versionId)
        set("lang", lang)
    }
}