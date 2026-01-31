package com.example.hardtracking

import java.time.Instant
import java.util.UUID

data class TimeEvent(
    val id: String = UUID.randomUUID().toString(),
    val start: Instant,
    val label: String = ""
)
