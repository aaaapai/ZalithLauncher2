package com.movtery.zalithlauncher.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.GLES20
import android.os.Process
import com.google.gson.GsonBuilder
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.utils.logging.Logger.lDebug
import com.movtery.zalithlauncher.utils.logging.Logger.lError
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.floor

val GSON = GsonBuilder().setPrettyPrinting().create()

/**
 * 格式化时间戳
 */
fun formatDate(
    date: Date,
    pattern: String = "yyyy-MM-dd HH:mm:ss",
    locale: Locale = Locale.getDefault(),
    timeZone: TimeZone = TimeZone.getDefault()
): String {
    val formatter = SimpleDateFormat(pattern, locale)
    formatter.timeZone = timeZone
    return formatter.format(date)
}

/**
 * 格式化时间戳
 */
fun formatDate(
    input: String,
    pattern: String = "yyyy-MM-dd HH:mm:ss",
    locale: Locale = Locale.getDefault(),
    zoneId: ZoneId = ZoneId.systemDefault()
): String {
    val formatter = DateTimeFormatter.ofPattern(pattern)
        .withLocale(locale)
        .withZone(zoneId)
    return formatter.format(
        OffsetDateTime.parse(input).toZonedDateTime()
    )
}

/**
 * 获取 xx 时间前 格式的字符串
 */
fun getTimeAgo(
    context: Context,
    dateString: String,
    inputFormat: String = "yyyy-MM-dd'T'HH:mm:ss[.SSS]'Z'"
): String {
    val formatter = DateTimeFormatter.ofPattern(inputFormat, Locale.getDefault())
        .withZone(ZoneOffset.UTC)

    val pastInstant = try {
        Instant.from(formatter.parse(dateString))
    } catch (_: DateTimeParseException) {
        try {
            Instant.parse(dateString)
        } catch (_: DateTimeParseException) {
            return ""
        }
    }

    val now = Instant.now()
    if (pastInstant.isAfter(now)) return context.getString(R.string.just_now)

    val pastZoned = pastInstant.atZone(ZoneId.systemDefault())
    val nowZoned = now.atZone(ZoneId.systemDefault())

    val years = ChronoUnit.YEARS.between(pastZoned, nowZoned)
    if (years > 0) return context.getString(R.string.years_ago, years)

    val months = ChronoUnit.MONTHS.between(pastZoned, nowZoned)
    if (months > 0) {
        //计算剩余天数
        val days = ChronoUnit.DAYS.between(
            pastZoned.plusMonths(months),
            nowZoned
        )
        return if (days > 0) {
            context.getString(R.string.months_days_ago, months, days)
        } else {
            context.getString(R.string.months_ago, months)
        }
    }

    val duration = Duration.between(pastInstant, now)
    val days = duration.toDays()
    if (days > 0) return context.getString(R.string.days_ago, days)

    val hours = duration.toHours()
    if (hours > 0) return context.getString(R.string.hours_ago, hours)

    val minutes = duration.toMinutes()
    if (minutes > 0) return context.getString(R.string.minutes_ago, minutes)

    return context.getString(R.string.just_now)
}

fun copyText(label: String?, text: String?, context: Context) {
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboardManager.setPrimaryClip(ClipData.newPlainText(label, text))
}

fun getSystemLanguage(): String {
    val locale = Locale.getDefault()
    return locale.language + "_" + locale.country.lowercase(Locale.getDefault())
}

fun getDisplayFriendlyRes(displaySideRes: Int, scaling: Float): Int {
    var display = (displaySideRes * scaling).toInt()
    if (display % 2 != 0) display--
    return display
}

fun isAdrenoGPU(): Boolean {
    val eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
    if (eglDisplay == EGL14.EGL_NO_DISPLAY) {
        lError("Failed to get EGL display")
        return false
    }

    if (!EGL14.eglInitialize(eglDisplay, null, 0, null, 0)) {
        lError("Failed to initialize EGL")
        return false
    }

    val eglAttributes = intArrayOf(
        EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
        EGL14.EGL_NONE
    )

    val configs = arrayOfNulls<EGLConfig>(1)
    val numConfigs = IntArray(1)
    if (!EGL14.eglChooseConfig(
            eglDisplay,
            eglAttributes,
            0,
            configs,
            0,
            1,
            numConfigs,
            0
        ) || numConfigs[0] == 0
    ) {
        EGL14.eglTerminate(eglDisplay)
        lError("Failed to choose an EGL config")
        return false
    }

    val contextAttributes = intArrayOf(
        EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
        EGL14.EGL_NONE
    )

    val context = EGL14.eglCreateContext(
        eglDisplay,
        configs[0]!!,
        EGL14.EGL_NO_CONTEXT,
        contextAttributes,
        0
    )
    if (context == EGL14.EGL_NO_CONTEXT) {
        EGL14.eglTerminate(eglDisplay)
        lError("Failed to create EGL context")
        return false
    }

    if (!EGL14.eglMakeCurrent(
            eglDisplay,
            EGL14.EGL_NO_SURFACE,
            EGL14.EGL_NO_SURFACE,
            context
        )
    ) {
        EGL14.eglDestroyContext(eglDisplay, context)
        EGL14.eglTerminate(eglDisplay)
        lError("Failed to make EGL context current")
        return false
    }

    val vendor = GLES20.glGetString(GLES20.GL_VENDOR)
    val renderer = GLES20.glGetString(GLES20.GL_RENDERER)
    val isAdreno = vendor != null && renderer != null &&
            vendor.equals("Qualcomm", ignoreCase = true) &&
            renderer.contains("adreno", ignoreCase = true)

    // Cleanup
    EGL14.eglMakeCurrent(
        eglDisplay,
        EGL14.EGL_NO_SURFACE,
        EGL14.EGL_NO_SURFACE,
        EGL14.EGL_NO_CONTEXT
    )
    EGL14.eglDestroyContext(eglDisplay, context)
    EGL14.eglTerminate(eglDisplay)

    lDebug("Running on Adreno GPU: $isAdreno")
    return isAdreno
}

fun killProgress() {
    runCatching {
        Process.killProcess(Process.myPid())
    }.onFailure {
        lError("Could not enable System.exit() method!", it)
    }
}

fun formatNumberByLocale(context: Context, number: Long): String {
    val locale = context.resources.configuration.locales.get(0)

    return when {
        isSimplifiedChinese(locale) || isTraditionalChinese(locale) -> formatChineseNumber(number)
        else -> formatNonChineseNumber(number)
    }
}

private fun isSimplifiedChinese(locale: Locale): Boolean {
    return locale.language == "zh" && (locale.country == "CN" || locale.script == "Hans")
}

private fun isTraditionalChinese(locale: Locale): Boolean {
    return locale.language == "zh" && (
            locale.country == "TW" || locale.country == "HK" || locale.script == "Hant"
            )
}

private fun formatChineseNumber(number: Long): String {
    return when {
        number < 10_000 -> number.toString()
        number < 100_000_000 -> {
            val value = number / 10_000.0
            formatWithUnit(value, "万")
        }
        else -> {
            val value = number / 100_000_000.0
            formatWithUnit(value, "亿")
        }
    }
}

private fun formatNonChineseNumber(number: Long): String {
    return when {
        number < 1_000 -> number.toString()
        number < 1_000_000 -> {
            val value = number / 1_000.0
            formatWithUnit(value, "K")
        }
        number < 1_000_000_000 -> {
            val value = number / 1_000_000.0
            formatWithUnit(value, "M")
        }
        else -> {
            val value = number / 1_000_000_000.0
            formatWithUnit(value, "B")
        }
    }
}

private fun formatWithUnit(value: Double, unit: String): String {
    val displayValue = if (value < 10) {
        String.format(Locale.US, "%.1f", value)
    } else {
        floor(value).toInt().toString()
    }
    return "$displayValue$unit"
}