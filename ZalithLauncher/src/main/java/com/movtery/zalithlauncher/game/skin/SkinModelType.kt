package com.movtery.zalithlauncher.game.skin

/**
 * 皮肤模型枚举
 */
enum class SkinModelType(val string: String, val targetParity: Int) {
    /** 未设定 */
    NONE("none", -1),
    /** 粗臂类型 */
    STEVE("wide", 0),
    /** 细臂类型 */
    ALEX("slim", 1)
}