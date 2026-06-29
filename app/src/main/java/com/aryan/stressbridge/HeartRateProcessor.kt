package com.aryan.stressbridge

import android.util.Log

class HeartRateProcessor {

    private var dcFilter = 0.0f
    private var isInitialized = false
    private var wasPositive = false

    private var lastBeatTimeMs = 0L
    private var currentBpm = 0.0f

    // 350ms = ~170 BPM Max, 2000ms = 30 BPM Min
    private val MIN_TIME_BETWEEN_BEATS_MS = 350
    private val MAX_TIME_BETWEEN_BEATS_MS = 2000

    fun processSample(rawPpg: Int): Float {
        val ppg = rawPpg.toFloat()

        if (!isInitialized) {
            dcFilter = ppg
            isInitialized = true
            return 0.0f
        }

        // 1) Slow-moving Low-Pass Filter to establish the wandering DC baseline
        dcFilter = (0.98f * dcFilter) + (0.02f * ppg)

        // 2) Subtract baseline to isolate the AC pulse wave (centered at 0)
        val acSignal = ppg - dcFilter

        // 3) Zero-Crossing Detection - current wave above 0
        val isPositive = acSignal > 0.0f

        // If it just crossed from negative to positive, then upward slope
        if (isPositive && !wasPositive) {
            val currentTime = System.currentTimeMillis()
            val timeSinceLastBeat = currentTime - lastBeatTimeMs

            // 4) Validate human biology limits
            if (timeSinceLastBeat in MIN_TIME_BETWEEN_BEATS_MS..MAX_TIME_BETWEEN_BEATS_MS) {

                val instantaneousBpm = 60000.0f / timeSinceLastBeat

                // exp moving avg to smooth UI
                if (currentBpm == 0.0f) {
                    currentBpm = instantaneousBpm
                } else {
                    currentBpm = (currentBpm * 0.7f) + (instantaneousBpm * 0.3f)
                }

                Log.d("APP_LOG", "BEAT DETECTED! BPM: $currentBpm")
                lastBeatTimeMs = currentTime

            } else if (timeSinceLastBeat > MAX_TIME_BETWEEN_BEATS_MS) {
                // reset timer if too long (e.g. watch was taken off)
                lastBeatTimeMs = currentTime
            }
        }

        // save state for next sample
        wasPositive = isPositive

        return currentBpm
    }
}