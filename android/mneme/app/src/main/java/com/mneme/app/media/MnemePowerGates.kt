package com.mneme.app.media

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import android.util.Log
import java.io.File

/**
 * Battery / thermal / RAM gates for heavy Retenqo work (LLM / OCR batch).
 * Lightweight triggers may still enqueue WorkManager; this decides whether to run.
 */
object MnemePowerGates {
    /** ~1.1 GB free — enough headroom before loading Qwen 2B + embed on 6 GB phones. */
    const val MIN_AVAILABLE_KB_HEAVY = 1_200_000L
    const val MIN_BATTERY_PCT = 20

    data class GateResult(
        val ok: Boolean,
        val reason: String,
    )

    fun canRunHeavyWork(context: Context, userInitiated: Boolean = false): GateResult {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (pm.isPowerSaveMode && !userInitiated) {
            return GateResult(false, "Power save on — deferred")
        }

        val bat = batteryPercent(context)
        val charging = isCharging(context)
        if (!userInitiated && !charging && bat in 0..MIN_BATTERY_PCT) {
            return GateResult(false, "Battery ${bat}% — wait until charging or >$MIN_BATTERY_PCT%")
        }

        val avail = memAvailableKb()
        if (avail > 0 && avail < MIN_AVAILABLE_KB_HEAVY) {
            return GateResult(
                false,
                "Low RAM (${avail / 1024} MB free) — free memory or try later",
            )
        }
        return GateResult(true, "ok bat=$bat% charging=$charging availMb=${avail / 1024}")
    }

    fun batteryPercent(context: Context): Int {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return -1
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || scale <= 0) return -1
        return (level * 100) / scale
    }

    fun isCharging(context: Context): Boolean {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return false
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
    }

    fun memAvailableKb(): Long {
        return try {
            File("/proc/meminfo").useLines { lines ->
                val line = lines.firstOrNull { it.startsWith("MemAvailable:") } ?: return -1L
                line.split(Regex("\\s+")).getOrNull(1)?.toLongOrNull() ?: -1L
            }
        } catch (e: Exception) {
            Log.w(TAG, "meminfo: ${e.message}")
            -1L
        }
    }

    private const val TAG = "MnemePower"
}
