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
            if (major >= 5 && minor >= 19) null
            else LibraryReplacement(
                newName = "net.java.dev.jna:jna:5.19.1",
                newPath = "net/java/dev/jna/jna/5.19.1/jna-5.19.1.jar",
                newSha1 = "ca303052cd617c1af2e2c8d344c98a706fb63143",
                newUrl = "https://repo1.maven.org/maven2/net/java/dev/jna/jna/5.19.1/jna-5.19.1.jar"
            )
        }
        libraryName.startsWith("com.github.oshi:oshi-core:") -> {
            if (major >= 7 && minor >= 4) null
            else LibraryReplacement(
                newName = "com.github.oshi:oshi-core:7.4.2",
                newPath = "com/github/oshi/oshi-core/7.4.2/oshi-core-7.4.2.jar",
                newSha1 = "0823528b4a54899104cefca7a1088ce4140687b0",
                newUrl = "https://repo1.maven.org/maven2/com/github/oshi/oshi-core/7.4.2/oshi-core-7.4.2.jar"
            )
        }
        libraryName.startsWith("org.ow2.asm:asm:") -> {
            if (major >= 9 && minor >= 10) null
            else LibraryReplacement(
                newName = "org.ow2.asm:asm:9.10.1",
                newPath = "org/ow2/asm/asm/9.10.1/asm-9.10.1.jar",
                newSha1 = "507e7f8821c58fc995a057b63ad4008b96bafa4d",
                newUrl = "https://repo1.maven.org/maven2/org/ow2/asm/asm/9.10.1/asm-9.10.1.jar"
            )
        }
        libraryName.startsWith("org.ow2.asm:asm-analysis:") -> {
            if (major >= 9 && minor >= 10) null
            else LibraryReplacement(
                newName = "org.ow2.asm:asm-analysis:9.10.1",
                newPath = "org/ow2/asm/asm-analysis/9.10.1/asm-analysis-9.10.1.jar",
                newSha1 = "cf2754cfa5e4df15ccf9f8779485f23862c5b488",
                newUrl = "https://repo1.maven.org/maven2/org/ow2/asm/asm-analysis/9.10.1/asm-analysis-9.10.1.jar"
            )
        }
        libraryName.startsWith("org.ow2.asm:asm-commons:") -> {
            if (major >= 9 && minor >= 10) null
            else LibraryReplacement(
                newName = "org.ow2.asm:asm-commons:9.10.1",
                newPath = "org/ow2/asm/asm-commons/9.10.1/asm-commons-9.10.1.jar",
                newSha1 = "a28039cb619a6c2a5c7ca5b3206381f9032c8368",
                newUrl = "https://repo1.maven.org/maven2/org/ow2/asm/asm-commons/9.10.1/asm-commons-9.10.1.jar"
            )
        }
        libraryName.startsWith("org.ow2.asm:asm-tree:") -> {
            if (major >= 9 && minor >= 10) null
            else LibraryReplacement(
                newName = "org.ow2.asm:asm-tree:9.10.1",
                newPath = "org/ow2/asm/asm-tree/9.10.1/asm-tree-9.10.1.jar",
                newSha1 = "0fe218ecae48cabf4e53295d64ae4e01e5ddfb33",
                newUrl = "https://repo1.maven.org/maven2/org/ow2/asm/asm-tree/9.10.1/asm-tree-9.10.1.jar"
            )
        }
        libraryName.startsWith("org.ow2.asm:asm-util:") -> {
            if (major >= 9 && minor >= 10) null
            else LibraryReplacement(
                newName = "org.ow2.asm:asm-util:9.10.1",
                newPath = "org/ow2/asm/asm-util/9.10.1/asm-util-9.10.1.jar",
                newSha1 = "c6db0976b79a615802b820b08664602fa1f6a5ad",
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
