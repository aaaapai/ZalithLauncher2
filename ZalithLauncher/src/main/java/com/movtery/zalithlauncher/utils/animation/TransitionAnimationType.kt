package com.movtery.zalithlauncher.utils.animation

import com.movtery.zalithlauncher.R

enum class TransitionAnimationType(val textRes: Int) {
    /** 关闭 */
    CLOSE(R.string.generic_close),
    /** 回弹 */
    JELLY_BOUNCE(R.string.animate_type_jelly_bounce),
    /** 弹跳 */
    BOUNCE(R.string.animate_type_bounce),
    /** 切入 */
    SLICE_IN(R.string.animate_type_slice_in)
}