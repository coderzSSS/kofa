package io.kofa.example.carnival.generated

import com.google.auto.service.AutoService
import io.kofa.platform.api.dsl.DomainDeclaration

@AutoService(DomainDeclaration::class)
class CarnivalDomainDeclaration: DomainDeclaration({
    domain = "carnival"
    pkg = "io.kofa.examples.carnival"

    module {
        includes(CarnivalModule.carnival())
    }
})