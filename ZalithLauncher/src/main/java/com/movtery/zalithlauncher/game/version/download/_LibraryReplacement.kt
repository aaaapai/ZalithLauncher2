/*
 * Zalith Launcher 2
 * Copyright (C) 2025 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.game.version.download

import com.movtery.zalithlauncher.game.versioninfo.models.GameManifest

data class LibraryReplacement(
    val newName: String,
    val newPath: String,
    val newSha1: String,
    val newUrl: String
)

fun getLibraryReplacement(libraryName: String, versionParts: List<String>): LibraryReplacement? {
    val major = versionParts.getOrNull(0)?.toIntOrNull() ?: 0
    val minor = versionParts.getOrNull(1)?.toIntOrNull() ?: 0

    return when {
        libraryName.startsWith("net.java.dev.jna:jna:") -> {
            //如果版本已经达到5.19.0及以上，则不做处理
            if (major >= 5 && minor >= 17) null
            else LibraryReplacement(
                newName = "net.java.dev.jna:jna:5.17.0",
                newPath = "net/java/dev/jna/jna/5.17.0/jna-5.17.0.jar",
                newSha1 = "33d12735bef894440780fce64f9758d420c7bae2",
                newUrl = "https://repo1.maven.org/maven2/net/java/dev/jna/jna/5.17.0/jna-5.17.0.jar"
            )
        }
        libraryName.startsWith("com.github.oshi:oshi-core:") -> {
            if (major >= 6 && minor >= 9) null
            else LibraryReplacement(
                newName = "com.github.oshi:oshi-core:6.9.0",
                newPath = "com/github/oshi/oshi-core/6.9.0/oshi-core-6.9.0.jar",
                newSha1 = "03224870731860cfcd7744581a05b559e94291e7",
                newUrl = "https://repo1.maven.org/maven2/com/github/oshi/oshi-core/6.9.0/oshi-core-6.9.0.jar"
            )
        }
        libraryName.startsWith("org.ow2.asm:asm:") -> {
            if (major >= 9 && minor >= 10) null
            else LibraryReplacement(
                newName = "org.ow2.asm:asm:9.10.1",
                newPath = "org/ow2/asm/asm/9.10.1/asm-9.10.1.jar",
                newSha1 = "ada2141c0cc52ee8f5c48cd5fa4ce0e794f22236",
                newUrl = "https://repo1.maven.org/maven2/org/ow2/asm/asm/9.10.1/asm-9.10.1.jar"
            )
        }
        libraryName.startsWith("org.ow2.asm:asm-analysis:") -> {
            if (major >= 9 && minor >= 10) null
            else LibraryReplacement(
                newName = "org.ow2.asm:asm-analysis:9.10.1",
                newPath = "org/ow2/asm/asm-analysis/9.10.1/asm-analysis-9.10.1.jar",
                newSha1 = "8d49f14d51f632cb1d87c88d1ceaf50db0d8af1b",
                newUrl = "https://repo1.maven.org/maven2/org/ow2/asm/asm-analysis/9.10.1/asm-analysis-9.10.1.jar"
            )
        }
        libraryName.startsWith("org.ow2.asm:asm-commons:") -> {
            if (major >= 9 && minor >= 10) null
            else LibraryReplacement(
                newName = "org.ow2.asm:asm-commons:9.10.1",
                newPath = "org/ow2/asm/asm-commons/9.10.1/asm-commons-9.10.1.jar",
                newSha1 = "4229e4c55fd8e01c23f9fe9884075cc628aacc50",
                newUrl = "https://repo1.maven.org/maven2/org/ow2/asm/asm-commons/9.10.1/asm-commons-9.10.1.jar"
            )
        }
        libraryName.startsWith("org.ow2.asm:asm-tree:") -> {
            if (major >= 9 && minor >= 10) null
            else LibraryReplacement(
                newName = "org.ow2.asm:asm-tree:9.10.1",
                newPath = "org/ow2/asm/asm-tree/9.10.1/asm-tree-9.10.1.jar",
                newSha1 = "e244332a17564c1d1572449399a842de35881be2",
                newUrl = "https://repo1.maven.org/maven2/org/ow2/asm/asm-tree/9.10.1/asm-tree-9.10.1.jar"
            )
        }
        libraryName.startsWith("org.ow2.asm:asm-util:") -> {
            if (major >= 9 && minor >= 10) null
            else LibraryReplacement(
                newName = "org.ow2.asm:asm-util:9.10.1",
                newPath = "org/ow2/asm/asm-util/9.10.1/asm-util-9.10.1.jar",
                newSha1 = "7bb9d450e8d4cbf9f9e04096c44bbfe7fba80b15",
                newUrl = "https://repo1.maven.org/maven2/org/ow2/asm/asm-util/9.10.1/asm-util-9.10.1.jar"
            )
        }
        libraryName.startsWith("org.ow2.asm:asm-all:") -> {
            //如果主版本号不低于5，则不做处理
            if (major >= 5) null
            else LibraryReplacement(
                newName = "org.ow2.asm:asm-all:5.2",
                newPath = "org/ow2/asm/asm-all/5.2/asm-all-5.2.jar",
                newSha1 = "d7443ae58d5479f99b5691041ccf0f4437f194e1",
                newUrl = "https://repo1.maven.org/maven2/org/ow2/asm/asm-all/5.2/asm-all-5.2.jar"
            )
        }
        else -> null
    }
}

/**
 * @return 是否需要被过滤
 */
fun GameManifest.Library.filterLibrary(): Boolean {
    return when {
        name?.contains("org.lwjgl") == true -> true
        name?.contains("jinput-platform") == true -> true
        name?.contains("twitch-platform") == true -> true
        else -> false
    }
}
