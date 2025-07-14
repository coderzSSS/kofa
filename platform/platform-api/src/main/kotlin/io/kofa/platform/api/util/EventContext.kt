package io.kofa.platform.api.util

interface EventContext {
    val eventType: Int
    val sourceSequence: Long
    val globalSequence: Long
    val source: String
    val eventTimeInEpochMilli: Long
}
