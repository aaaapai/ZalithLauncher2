package com.movtery.zalithlauncher.utils.file

import java.util.zip.ZipEntry

class JavaZipEntryAdapter(val entry: ZipEntry) : ZipEntryBase {
    override val name: String = entry.name
    override val isDirectory: Boolean = entry.isDirectory
}