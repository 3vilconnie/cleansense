package com.cleansense.app.sensor

import kotlin.math.roundToInt

enum class AirStatus {
    /** Primeras lecturas: se mide el "aire de referencia". */
    CALIBRATING,
    /** Aire normal, sin olores. */
    NORMAL,
    /** Olores / compuestos orgánicos volátiles (COV) cerca del sensor. */
    VOC,
    /** Después de detectar COV, el aire va volviendo a la normalidad. */
    RECOVERING,
}

data class AirReading(
    val status: AirStatus,
    val raw: Int? = null,
    val baseline: Int? = null,
    val calibrationProgress: Float = 0f,
)

class AirQualityAnalyzer(
    private val calibrationSamples: Int = 10,
    private val vocDelta: Double = 15.0,      // ← Con +15 o más pasa a VOC ("Mandar a lavar")
    private val recoverDelta: Double = 10.0,  // ← Al bajar de +10 pasa a RECOVERING ("Volviendo a niveles base")
    private val normalDelta: Double = 5.0,    // ← Al bajar de +5 vuelve a NORMAL ("Aire normal")
    private val smoothing: Double = 0.45,
    private val baselineWindowMs: Long = 30_000L,
) {
    private val calibration = mutableListOf<Int>()
    private var baseline: Double? = null
    private var smoothed: Double? = null

    private var windowStartTime: Long = 0L
    private var windowMin: Double? = null

    var current: AirReading = AirReading(AirStatus.CALIBRATING)
        private set

    fun reset() {
        calibration.clear()
        baseline = null
        smoothed = null
        windowStartTime = 0L
        windowMin = null
        current = AirReading(AirStatus.CALIBRATING)
    }

    fun process(raw: Int): AirReading {
        if (raw <= 0) return current

        val value = smoothed?.let { it + smoothing * (raw - it) } ?: raw.toDouble()
        smoothed = value

        val base = baseline
        if (base == null) {
            calibration += raw
            current = if (calibration.size >= calibrationSamples) {
                val initialMin = calibration.min().toDouble()
                baseline = initialMin
                windowStartTime = System.currentTimeMillis()
                windowMin = initialMin
                AirReading(AirStatus.NORMAL, raw, initialMin.roundToInt(), 1f)
            } else {
                AirReading(
                    status = AirStatus.CALIBRATING,
                    raw = raw,
                    calibrationProgress = calibration.size / calibrationSamples.toFloat()
                )
            }
            return current
        }

        val currentWindowMin = windowMin
        if (currentWindowMin == null || value < currentWindowMin) {
            windowMin = value
        }

        val now = System.currentTimeMillis()
        var currentBase: Double = base
        if (now - windowStartTime >= baselineWindowMs) {
            val minInWindow = windowMin
            if (minInWindow != null && minInWindow < currentBase) {
                currentBase = minInWindow
                baseline = currentBase
            }
            windowStartTime = now
            windowMin = value
        }

        val delta = value - currentBase
        val status = when (current.status) {
            AirStatus.VOC ->
                if (delta < recoverDelta) AirStatus.RECOVERING else AirStatus.VOC
            AirStatus.RECOVERING -> when {
                delta >= vocDelta -> AirStatus.VOC
                delta <= normalDelta -> AirStatus.NORMAL
                else -> AirStatus.RECOVERING
            }
            else ->
                if (delta >= vocDelta) AirStatus.VOC else AirStatus.NORMAL
        }

        current = AirReading(status, raw, currentBase.roundToInt(), 1f)
        return current
    }

    companion object {
        private val NUMBER = Regex("-?\\d+(\\.\\d+)?")

        fun parse(line: String): Int? =
            NUMBER.find(line)?.value?.toDoubleOrNull()?.roundToInt()
    }
}