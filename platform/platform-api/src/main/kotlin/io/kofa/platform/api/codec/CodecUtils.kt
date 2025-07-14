package io.kofa.platform.api.codec

import org.apache.commons.text.WordUtils

object CodecUtils {
    fun generateSourceAbbr(source: String): String {
        val abbr = WordUtils.abbreviate(source, 1, Int.SIZE_BYTES, "_")
        val max = Int.SIZE_BYTES.coerceAtMost(abbr.length)

        return abbr.substring(0, max)
    }

    fun encodeSourceAbbrToInt(abbr: String): Int {
        check(abbr.length <= Int.SIZE_BYTES) {
            "string too long, should be less or equal to ${Int.SIZE_BYTES}: $abbr"
        }

        var value: Int = 0
        for (i in 0 until abbr.length) {
            val c = abbr[i]
            value = (value shl 8) + (c.code and 0xFF)
        }

        return value
    }

    fun encodeSourceToInt(source: String): Int {
        return encodeSourceAbbrToInt(generateSourceAbbr(source))
    }

    fun decodeIntToSource(value: Int): String {
        val chars = ByteArray(Int.SIZE_BYTES)
        val max = chars.size

        var size = 0
        var num = value
        for(i in 0 until max) {
            val byte = (num and 0xFF).toByte()
            if (byte.toInt() == 0) {
                break
            }

            chars[max - 1 -i] = byte
            size = size + 1
            num = num shr 8
        }

        return String(chars.slice((max - size).. (max -1)).toByteArray(), Charsets.US_ASCII)
    }
}