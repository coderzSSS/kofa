package io.kofa.example.carnival.generated

import io.kofa.example.carnival.BananaDecoder
import io.kofa.example.carnival.BananaEncoder
import io.kofa.example.carnival.MessageHeaderDecoder
import io.kofa.example.carnival.MessageHeaderEncoder
import io.kofa.example.carnival.domain.message.CarnivalEvent.Banana
import io.kofa.platform.api.codec.DirectBufferCodec
import org.agrona.DirectBuffer
import org.agrona.MutableDirectBuffer

class CarnivalMessageCodec : DirectBufferCodec {
    private val bananaEncoder = BananaEncoder()
    private val bananaDecoder = BananaDecoder()

    private val messageHeaderEncoder = MessageHeaderEncoder()
    private val messageHeaderDecoder = MessageHeaderDecoder()

    override fun <T> encodeToDirectBuffer(value: T, byteBuffer: MutableDirectBuffer, offset: Int): Int {
        when(value) {
            is Banana -> {
                //write the encoded output to the direct buffer
                bananaEncoder.wrapAndApplyHeader(byteBuffer, offset, messageHeaderEncoder);

                bananaEncoder.name(value.name)

                return MessageHeaderEncoder.ENCODED_LENGTH + bananaEncoder.encodedLength();
            }
            else -> throw IllegalStateException("unknown value type $value")
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> decodeFromDirectBuffer(byteBuffer: DirectBuffer, offset: Int): T {
        messageHeaderDecoder.wrap(byteBuffer, offset);

        // Lookup the applicable flyweight to decode this type of message based on templateId and version.
        val templateId = messageHeaderDecoder.templateId();
        val actingBlockLength = messageHeaderDecoder.blockLength();
        val actingVersion = messageHeaderDecoder.version();

        var bufferOffset = messageHeaderDecoder.encodedLength() + offset;

        when (templateId) {
            BananaDecoder.TEMPLATE_ID -> {
                bananaDecoder.wrap(byteBuffer, bufferOffset, actingBlockLength, actingVersion);

                return Banana(name = bananaDecoder.name()) as T
            }
            else -> throw IllegalStateException("unknown templateId $templateId")
        }
    }
}