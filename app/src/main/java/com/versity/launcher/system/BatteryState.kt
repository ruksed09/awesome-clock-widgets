package com.versity.launcher.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.roundToInt

/** Maps raw level/scale to 0..100, or null when either value is missing or invalid. */
fun batteryPercent(level: Int, scale: Int): Int? {
    if (level < 0 || scale <= 0) return null
    return (level * 100f / scale).roundToInt().coerceIn(0, 100)
}

private fun Intent?.toPercent(): Int? = this?.let {
    batteryPercent(
        it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1),
        it.getIntExtra(BatteryManager.EXTRA_SCALE, -1),
    )
}

/**
 * Emits the battery percentage while collected. Uses the sticky ACTION_BATTERY_CHANGED
 * broadcast, so there is no background service and nothing runs while the UI is stopped.
 */
fun batteryPercentFlow(context: Context): Flow<Int?> = callbackFlow {
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) { trySend(intent.toPercent()) }
    }
    val sticky = ContextCompat.registerReceiver(
        context, receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED,
    )
    trySend(sticky.toPercent())
    awaitClose { context.unregisterReceiver(receiver) }
}.distinctUntilChanged()
