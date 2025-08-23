package com.movtery.zalithlauncher.game.version.mod

import com.tencent.mmkv.MMKV

/**
 * 模组项目缓存 MMKV，文件 HASH 值对应项目
 */
fun modProjectCache(): MMKV = MMKV.mmkvWithID("ModProjectHashMapper", MMKV.MULTI_PROCESS_MODE)
/**
 * 模组版本缓存 MMKV，文件 HASH 值对应版本
 */
fun modVersionCache(): MMKV = MMKV.mmkvWithID("ModVersionHashMapper", MMKV.MULTI_PROCESS_MODE)
