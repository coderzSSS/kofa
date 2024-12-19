package io.kofa.platform.api.codec

import org.agrona.DirectBuffer
import org.agrona.MutableDirectBuffer

interface DirectBufferCodec {
    fun <T> encodeToDirectBuffer(value: T, buffer: MutableDirectBuffer, offset: Int): Int

    fun <T> decodeFromDirectBuffer(buffer: DirectBuffer, offset: Int): T
}