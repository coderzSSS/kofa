package io.kofa.example.carnival.generated

import io.kofa.platform.api.codec.DirectBufferCodec
import io.kofa.platform.api.meta.MessageMetaProvider
import org.koin.dsl.bind
import org.koin.dsl.module

object CarnivalModule {
    fun carnival() = module {
        factory {
            CarnivalMessageCodec()
        }.bind(DirectBufferCodec::class)

        single {
            CarnivalMessageConstants
        }.bind(MessageMetaProvider::class)
    }
}
