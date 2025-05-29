package com.movtery.zalithlauncher.components

import android.content.Context
import com.movtery.zalithlauncher.path.PathManager

class UnpackComponentsTask(context: Context, val component: Components) : UnpackSingleTask(
    context = context,
    rootDir = PathManager.DIR_COMPONENTS,
    assetsDirName = "components",
    fileDirName = component.component
)