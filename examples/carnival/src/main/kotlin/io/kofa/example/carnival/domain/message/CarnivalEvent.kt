package io.kofa.example.carnival.domain.message

sealed interface CarnivalEvent {
    data class Banana(val name: String): CarnivalEvent

    data class Apple(val name: String): CarnivalEvent
}
