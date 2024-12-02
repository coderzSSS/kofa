package io.kofa.example.carnival.domain

import com.google.auto.service.AutoService
import io.kofa.example.carnival.domain.message.Banana
import io.kofa.example.carnival.domain.message.ShowStarted
import io.kofa.platform.api.dsl.DomainDeclaration

@AutoService(DomainDeclaration::class)
object Carnival: DomainDeclaration({
    domain = "carnival"

    //domain(CarnivalEvent::class)
    message(1, Banana::class)
    message(2, ShowStarted::class)

    fixCodec(Banana::class) {
        mapping(Banana::name, "123")
    }
})