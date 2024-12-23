package io.kofa.example.carnival.generated

import com.google.auto.service.AutoService
import io.kofa.example.carnival.application.ClownModule.clownConfig
import io.kofa.example.carnival.application.ClownModule.clownHandler
import io.kofa.platform.api.dsl.BusinessDeclaration
import io.kofa.platform.api.dsl.inject

@AutoService(BusinessDeclaration::class)
@SuppressWarnings("rawtypes")
class CarnivalBusinessDeclaration : BusinessDeclaration<Any>({
    component("Clown2") {
        install(clownConfig(), clownHandler())

        withEventDispatcher(inject<CarnivalMessageHandler>())
    }
})