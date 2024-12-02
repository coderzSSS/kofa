package io.kofa.example.carnival.domain.message

import kotlinx.datetime.Instant

data class ShowStarted(val timestamp: Instant) : CarnivalEvent