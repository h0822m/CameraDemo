package com.xw.cameraxdemo.util

import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.nio.charset.CharsetDecoder

/**
 * Create by zcr on 2023/2/3
 */
object ByteUtils {
    fun byteBufferToString(buffer: ByteBuffer): String? {
        var charBuffer: CharBuffer? = null
        return try {
            val charset: Charset = Charset.forName("UTF-8")
            val decoder: CharsetDecoder = charset.newDecoder()
            charBuffer = decoder.decode(buffer)
            buffer.flip()
            charBuffer.toString()
        } catch (ex: Exception) {
            ex.printStackTrace()
            null
        }
    }
}