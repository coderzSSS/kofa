package io.kofa.platform.api.codec

import io.kofa.platform.api.message.EventHeader
import org.agrona.DirectBuffer
import org.agrona.MutableDirectBuffer

interface DirectBufferCodec {
    fun <T> encodeToDirectBuffer(header: EventHeader, value: T, buffer: MutableDirectBuffer, offset: Int): Int

    fun <T> decodeFromDirectBuffer(buffer: DirectBuffer, offset: Int): Pair<EventHeader, T>
}