package com.movtery.zalithlauncher.utils.file

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jackhuang.hmcl.util.DigestUtils
import org.junit.Test
import java.io.File

class FileTest {

    @Test
    fun testCalculateFileSha1() {
        val file = File("F:\\Download\\geckolib-forge-1.21.8-5.2.2.jar")
        runBlocking(Dispatchers.IO) {
            val sha11 = calculateFileSha1(file)
            println("sha1 1 = $sha11")
            val sha12 = DigestUtils.digestToString("SHA-1", file.toPath())
            println("sha1 2 = $sha12")
        }
    }
}