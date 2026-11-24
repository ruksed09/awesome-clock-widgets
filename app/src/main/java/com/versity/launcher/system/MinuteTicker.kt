package com.versity.launcher.system

import java.time.LocalDateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Emits the current time now and then at the start of every minute, only while collected. */
fun minuteTicker(now: () -> LocalDateTime = LocalDateTime::now): Flow<LocalDateTime> = flow {
    while (true) {
        val t = now()
        emit(t)
        delay(60_000L - (t.second * 1_000L + t.nano / 1_000_000L) + 50L)
    }
}
