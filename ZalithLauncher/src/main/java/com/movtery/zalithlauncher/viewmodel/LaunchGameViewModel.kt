package com.movtery.zalithlauncher.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.ui.screens.content.elements.LaunchGameOperation

class LaunchGameViewModel : ViewModel() {
    /**
     * 启动游戏操作状态
     */
    var launchGameOperation by mutableStateOf<LaunchGameOperation>(LaunchGameOperation.None)
        private set

    /**
     * 尝试启动游戏
     */
    fun tryLaunch(
        version: Version?
    ) {
        if (launchGameOperation == LaunchGameOperation.None) {
            launchGameOperation = LaunchGameOperation.TryLaunch(version)
        }
    }

    /**
     * 快速启动（通过存档管理快速游玩存档）
     * @param saveName 存档文件名称
     */
    fun quickLaunch(
        version: Version,
        saveName: String
    ) {
        if (launchGameOperation == LaunchGameOperation.None) {
            launchGameOperation = LaunchGameOperation.TryLaunch(version, saveName)
        }
    }

    fun updateOperation(operation: LaunchGameOperation) {
        this.launchGameOperation = operation
    }
}