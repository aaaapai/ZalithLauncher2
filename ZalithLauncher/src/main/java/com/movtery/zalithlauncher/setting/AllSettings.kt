package com.movtery.zalithlauncher.setting

import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.movtery.zalithlauncher.game.path.GamePathManager
import com.movtery.zalithlauncher.info.InfoDistributor
import com.movtery.zalithlauncher.setting.enums.GestureActionType
import com.movtery.zalithlauncher.setting.enums.MirrorSourceType
import com.movtery.zalithlauncher.setting.enums.MouseControlMode
import com.movtery.zalithlauncher.ui.theme.ColorThemeType
import com.movtery.zalithlauncher.utils.animation.TransitionAnimationType

object AllSettings : SettingsRegistry() {
    //Renderer
    /**
     * 全局渲染器
     */
    val renderer = stringSetting("renderer", "")

    /**
     * Vulkan 驱动器
     */
    val vulkanDriver = stringSetting("vulkanDriver", "default turnip")

    /**
     * 分辨率
     */
    val resolutionRatio = intSetting("resolutionRatio", 100)

    /**
     * 游戏页面全屏化
     */
    val gameFullScreen = boolSetting("gameFullScreen", true)

    /**
     * 持续性能模式
     */
    val sustainedPerformance = boolSetting("sustainedPerformance", false)

    /**
     * 使用系统的 Vulkan 驱动
     */
    val zinkPreferSystemDriver = boolSetting("zinkPreferSystemDriver", false)

    /**
     * Zink 垂直同步
     */
    val vsyncInZink = boolSetting("vsyncInZink", false)

    /**
     * 强制在高性能核心运行
     */
    val bigCoreAffinity = boolSetting("bigCoreAffinity", false)

    /**
     * 启用着色器日志输出
     */
    val dumpShaders = boolSetting("dumpShaders", false)

    //Game
    /**
     * 版本隔离
     */
    val versionIsolation = boolSetting("versionIsolation", true)

    /**
     * 不检查游戏完整性
     */
    val skipGameIntegrityCheck = boolSetting("skipGameIntegrityCheck", false)

    /**
     * 版本自定义信息
     */
    val versionCustomInfo = stringSetting("versionCustomInfo", "${InfoDistributor.LAUNCHER_IDENTIFIER}[zl_version]")

    /**
     * 启动器的Java环境
     */
    val javaRuntime = stringSetting("javaRuntime", "")

    /**
     * 自动选择Java环境
     */
    val autoPickJavaRuntime = boolSetting("autoPickJavaRuntime", true)

    /**
     * 游戏内存分配大小
     */
    val ramAllocation = intSetting("ramAllocation", -1)

    /**
     * 自定义Jvm启动参数
     */
    val jvmArgs = stringSetting("jvmArgs", "")

    /**
     * 启动游戏时自动展示日志，直到游戏开始渲染
     */
    val showLogAutomatic = boolSetting("showLogAutomatic", false)

    /**
     * 日志字体大小
     */
    val logTextSize = intSetting("logTextSize", 15)

    /**
     * 日志缓冲区刷新时间
     */
    val logBufferFlushInterval = intSetting("logBufferFlushInterval", 200)

    //Control
    /**
     * 实体鼠标控制
     */
    val physicalMouseMode = boolSetting("physicalMouseMode", true)

    /**
     * 按键键值，按下按键呼出输入法
     */
    val physicalKeyImeCode = intSetting("physicalKeyImeCode", null)

    /**
     * 隐藏虚拟鼠标
     */
    val hideMouse = boolSetting("hideMouse", false)

    /**
     * 虚拟鼠标大小（Dp）
     */
    val mouseSize = intSetting("mouseSize", 24)

    /**
     * 虚拟鼠标灵敏度
     */
    val cursorSensitivity = intSetting("cursorSensitivity", 100)

    /**
     * 被抓获指针移动灵敏度
     */
    val mouseCaptureSensitivity = intSetting("mouseCaptureSensitivity", 100)

    /**
     * 虚拟鼠标控制模式
     */
    val mouseControlMode = enumSetting("mouseControlMode", MouseControlMode.SLIDE)

    /**
     * 鼠标控制长按延迟
     */
    val mouseLongPressDelay = intSetting("mouseLongPressDelay", 300)

    /**
     * 手势控制
     */
    val gestureControl = boolSetting("gestureControl", false)

    /**
     * 手势控制点击时触发的鼠标按钮
     */
    val gestureTapMouseAction = enumSetting("gestureTapMouseAction", GestureActionType.MOUSE_RIGHT)

    /**
     * 手势控制长按时触发的鼠标按钮
     */
    val gestureLongPressMouseAction = enumSetting("gestureLongPressMouseAction", GestureActionType.MOUSE_LEFT)

    /**
     * 手势控制长按延迟
     */
    val gestureLongPressDelay = intSetting("gestureLongPressDelay", 300)

    //Launcher
    /**
     * 颜色主题色
     * Android 12+ 默认动态主题色
     */
    val launcherColorTheme = enumSetting(
        "launcherColorTheme",
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) ColorThemeType.DYNAMIC
        else ColorThemeType.EMBERMIRE
    )

    /**
     * 自定义颜色主题色
     */
    val launcherCustomColor = intSetting("launcherCustomColor", Color.Blue.toArgb())

    /**
     * 启动器部分屏幕全屏
     */
    val launcherFullScreen = boolSetting("launcherFullScreen", true)

    /**
     * 动画倍速
     */
    val launcherAnimateSpeed = intSetting("launcherAnimateSpeed", 5)

    /**
     * 动画幅度
     */
    val launcherAnimateExtent = intSetting("launcherAnimateExtent", 5)

    /**
     * 启动器页面切换动画类型
     */
    val launcherSwapAnimateType = enumSetting("launcherSwapAnimateType", TransitionAnimationType.JELLY_BOUNCE)

    /**
     * 启动器日志保留天数
     */
    val launcherLogRetentionDays = intSetting("launcherLogRetentionDays", 7)

    /**
     * 下载版本附加内容镜像源类型
     */
    val fetchModLoaderSource = enumSetting("fetchModLoaderSource", MirrorSourceType.OFFICIAL_FIRST)

    /**
     * 文件下载镜像源类型
     */
    val fileDownloadSource = enumSetting("fileDownloadSource", MirrorSourceType.OFFICIAL_FIRST)

    //Other
    /**
     * 当前选择的账号
     */
    val currentAccount = stringSetting("currentAccount", "")

    /**
     * 当前选择的游戏目录id
     */
    val currentGamePathId = stringSetting("currentGamePathId", GamePathManager.DEFAULT_ID)

    /**
     * 启动器任务菜单是否展开
     */
    val launcherTaskMenuExpanded = boolSetting("launcherTaskMenuExpanded", true)

    /**
     * 在游戏菜单悬浮窗上显示帧率
     */
    val showFPS = boolSetting("showFPS", true)
}