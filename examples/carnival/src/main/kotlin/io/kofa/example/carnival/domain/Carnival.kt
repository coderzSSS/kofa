package io.kofa.example.carnival.domain

import com.google.auto.service.AutoService
import io.kofa.example.carnival.domain.message.CarnivalEvent.*
import io.kofa.platform.api.dsl.DomainDeclaration

@AutoService(DomainDeclaration::class)
class Carnival: DomainDeclaration({
    domain = "carnival"
    pkg = "io.kofa.examples.carnival"

    //domain(CarnivalEvent::class)
    message(1, Banana::class)
    message(2, Apple::class)

    fixCodec(Banana::class) {
        mapping(Banana::name, "123")
    }
})