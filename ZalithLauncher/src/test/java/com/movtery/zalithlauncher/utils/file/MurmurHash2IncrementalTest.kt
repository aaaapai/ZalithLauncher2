package com.movtery.zalithlauncher.utils.file

import org.apache.commons.codec.digest.MurmurHash2
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files

class MurmurHash2IncrementalTest {

    @Test
    fun testTwoWay() {
        val file = File("F:\\Download\\geckolib-forge-1.21.8-5.2.2.jar")
        val hash1 = way1(file)
        println("Way 1 hash = $hash1")
        val hash2 = way2(file)
        println("Way 2 hash = $hash2")
    }

    //Old
    private fun way1(file: File): Long {
        val baos = ByteArrayOutputStream()
        Files.newInputStream(file.toPath()).use { stream ->
            val buf = ByteArray(1024)
            var bytesRead: Int
            while (stream.read(buf).also { bytesRead = it } != -1) {
                for (i in 0 until bytesRead) {
                    val b = buf[i]
                    if (b.toInt() !in listOf(0x9, 0xa, 0xd, 0x20)) {
                        baos.write(b.toInt())
                    }
                }
            }
        }
        return Integer.toUnsignedLong(MurmurHash2.hash32(baos.toByteArray(), baos.size(), 1))
    }

    private fun way2(file: File): Long {
        return MurmurHash2Incremental.computeHash(file, byteToSkip = listOf(0x9, 0xa, 0xd, 0x20))
    }
}