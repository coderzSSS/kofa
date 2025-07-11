package io.kofa.platform.api.message

data class EventHeader(
    val eventType: Int,
    val source: String,
    val sourceSequence: Long,
    val globalSequence: Long,
    val eventTimeStampInMillis: Long
)