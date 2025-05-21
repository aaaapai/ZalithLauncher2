package com.movtery.zalithlauncher.game.skin

import com.movtery.zalithlauncher.game.version.installed.VersionInfo

private fun getLocalUuid(name: String): String {
    val lengthPart = name.length
        .toString(16)
        .padStart(16, '0')
        .takeLast(16)  //确保超长时取最后16位

    //无符号32位 → 8位十六进制 → 右侧补零到16位
    val hashCode = (name.hashCode().toLong() and 0xFFFFFFFFL)
    val hashPart = hashCode.toString(16)
        .padStart(8, '0')
        .padEnd(16, '0')
        .takeLast(16) //确保最长16位

    return buildString(34) {
        append(lengthPart.substring(0, 12))
        append('3')
        append(lengthPart[12])
        append(hashPart.substring(0, 3))
        append('9')
        append(hashPart.substring(3))
    }
}

/**
 * 根据皮肤模型类型，生成 profileId
 */
fun getLocalUUIDWithSkinModel(userName: String, skinModelType: SkinModelType): String {
    val baseUuid = getLocalUuid(userName)
    if (skinModelType == SkinModelType.NONE) return baseUuid

    val prefix = baseUuid.substring(0, 27)
    val a = baseUuid[7].digitToInt(16)
    val b = baseUuid[15].digitToInt(16)
    val c = baseUuid[23].digitToInt(16)

    var suffix = baseUuid.substring(27).toLong(16)
    val maxSuffix = 0xFFFFFL

    repeat(maxSuffix.toInt() + 1) {
        val currentD = (suffix and 0xFL).toInt()
        if ((a xor b xor c xor currentD) % 2 == skinModelType.targetParity) {
            return prefix + suffix.toString(16).padStart(5, '0').uppercase()
        }
        suffix = if (suffix == maxSuffix) 0L else suffix + 1
    }

    return prefix + suffix.toString(16).padStart(5, '0').uppercase()
}

/**
 * @return 当前版本是否支持使用皮肤模型自定义
 */
fun isSkinModelUuidSupported(versionInfo: VersionInfo): Boolean {
    val code = versionInfo.getMcVersionCode()
    return code.main in 2..7
}

/**
 * @return 当前版本是否兼容离线账号自定义皮肤包
 */
fun isOfflineSkinCompatible(versionInfo: VersionInfo): Boolean {
    val code = versionInfo.getMcVersionCode()
    //      1.0 ~ 1.5                   1.19.3+
    return code.main in 0..5 || (code.main == 19 && code.sub > 2) || code.main > 19
}