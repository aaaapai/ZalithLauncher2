package com.movtery.zalithlauncher.setting

import com.movtery.zalithlauncher.info.InfoDistributor
import com.tencent.mmkv.MMKV

/**
 * 启动器全局 MMKV，管理所有设置项
 */
fun launcherMMKV(): MMKV = MMKV.mmkvWithID(InfoDistributor.LAUNCHER_IDENTIFIER, MMKV.MULTI_PROCESS_MODE)