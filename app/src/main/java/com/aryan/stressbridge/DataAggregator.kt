package com.aryan.stressbridge

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class StressDataRecord(
    val timestamp: Long,
    val rawPpg: Float,
    val motion: Float,
    val gsr: Float
)

class DataAggregator {
    private var gsrSum = 0.0f
    private var motionSum = 0.0f
    private var ppgSum = 0.0f
    private var sampleCount = 0
    private var lastSecondTimestamp = System.currentTimeMillis() / 1000

    private val _recordFlow = MutableSharedFlow<StressDataRecord>()
    val recordFlow: SharedFlow<StressDataRecord> = _recordFlow.asSharedFlow()

    private var hasEmittedForCurrentSecond = false

    suspend fun addData(gsr: Float, motion: Float, ppg: Float) {
        val currentSecond = System.currentTimeMillis() / 1000

        if (currentSecond > lastSecondTimestamp) {
            hasEmittedForCurrentSecond = false // reset the flag
            lastSecondTimestamp = currentSecond
        }

        if (!hasEmittedForCurrentSecond && sampleCount > 0) {
            val record = StressDataRecord(
                timestamp = lastSecondTimestamp,
                rawPpg = ppgSum / sampleCount,
                motion = motionSum / sampleCount,
                gsr = gsrSum / sampleCount
            )
            _recordFlow.emit(record)
            hasEmittedForCurrentSecond = true // mark as emitted

            // Reset counters
            gsrSum = 0.0f
            motionSum = 0.0f
            ppgSum = 0.0f
            sampleCount = 0
        }

        gsrSum += gsr
        motionSum += motion
        ppgSum += ppg
        sampleCount++
    }
}