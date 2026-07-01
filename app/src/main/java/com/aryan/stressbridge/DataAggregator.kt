package com.aryan.stressbridge

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class StressDataRecord(
    val timestamp: String,
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

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val _recordFlow = MutableSharedFlow<StressDataRecord>()
    val recordFlow: SharedFlow<StressDataRecord> = _recordFlow.asSharedFlow()

    private var hasEmittedForCurrentSecond = false

    suspend fun addData(gsr: Float, motion: Float, ppg: Float) {
        val currentTime = System.currentTimeMillis()
        val currentSecond = currentTime / 1000

        if (currentSecond > lastSecondTimestamp) {
            hasEmittedForCurrentSecond = false
            lastSecondTimestamp = currentSecond
        }

        if (!hasEmittedForCurrentSecond && sampleCount > 0) {
            // Generate the formatted string here
            val formattedTimestamp = dateFormatter.format(Date(lastSecondTimestamp * 1000))

            val record = StressDataRecord(
                timestamp = formattedTimestamp, // Now a String
                rawPpg = ppgSum / sampleCount,
                motion = motionSum / sampleCount,
                gsr = gsrSum / sampleCount
            )
            _recordFlow.emit(record)
            hasEmittedForCurrentSecond = true

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