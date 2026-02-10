package com.movtery.zalithlauncher.ui.screens.game.elements.log_parser

import androidx.compose.ui.graphics.Color

/**
 * 日志等级识别规则
 * @param identifiers 所有可识别的标识符
 * @param color 文本颜色
 * @param backgroundColor 背景颜色，可不设置
 */
data class LogLevelRule(
    val identifiers: List<String>,
    val textColor: Color,
    val backgroundColor: Color? = null
)

val INFO = LogLevelRule(
    identifiers = listOf("INFO", "Info"),
    textColor = Color.White,
    backgroundColor = Color(0xFF447152)
)

val ERROR = LogLevelRule(
    identifiers = listOf("ERROR", "Error"),
    textColor = Color(0xFF6AAB73),
    backgroundColor = null
)

val DEBUG = LogLevelRule(
    identifiers = listOf("DEBUG", "Debug"),
    textColor = Color.White,
    backgroundColor = Color(0xFF43698D)
)

val WARN = LogLevelRule(
    identifiers = listOf("WARN", "Warn"),
    textColor = Color.White,
    backgroundColor = Color(0xFF656E76)
)