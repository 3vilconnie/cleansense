package com.cleansense.app.sensor

import kotlin.math.abs
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

/**
 * Convierte las lecturas analógicas del MQ-135 (0 a 1023) en un estado de aire.
 *
 * El MQ-135 entrega un valor más alto cuando hay más gases/olores (COV, alcohol,
 * amoníaco, humo, etc.). Como cada sensor es distinto, la app primero toma
 * [calibrationSamples] lecturas para conocer el valor del aire limpio (línea base)
 * y luego compara cada lectura contra esa base:
 *
 *  - subida ≥ [vocDelta]                 → VOC (olores detectados)
 *  - estaba en VOC y baja de [recoverDelta] → RECOVERING (volviendo a niveles base)
 *  - en RECOVERING y baja de [normalDelta]  → NORMAL
 *
 * Ajusta los umbrales según tu sensor (ver README).
 */
class AirQualityAnalyzer(
    private val calibrationSamples: Int = 10,
    private val vocDelta: Double = 60.0,
    private val recoverDelta: Double = 45.0,
    private val normalDelta: Double = 20.0,
    private val smoothing: Double = 0.35,
    private val baselineDrift: Double = 0.02,
) {
    private val calibration = mutableListOf<Int>()
    private var baseline: Double? = null
    private var smoothed: Double? = null

    var current: AirReading = AirReading(AirStatus.CALIBRATING)
        private set

    fun reset() {
        calibration.clear()
        baseline = null
        smoothed = null
        current = AirReading(AirStatus.CALIBRATING)
    }

    fun process(raw: Int): AirReading {
        // Media móvil exponencial: evita que el estado "parpadee" por ruido.
        val value = smoothed?.let { it + smoothing * (raw - it) } ?: raw.toDouble()
        smoothed = value

        val base = baseline
        if (base == null) {
            calibration += raw
            current = if (calibration.size >= calibrationSamples) {
                val avg = calibration.average()
                baseline = avg
                AirReading(AirStatus.NORMAL, raw, avg.roundToInt(), 1f)
            } else {
                AirReading(
                    status = AirStatus.CALIBRATING,
                    raw = raw,
                    calibrationProgress = calibration.size / calibrationSamples.toFloat()
                )
            }
            return current
        }

        val delta = value - base
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

        // Con aire normal la línea base sigue lentamente la deriva del sensor
        // (temperatura, humedad, calentamiento).
        var newBase = base
        if (status == AirStatus.NORMAL && (delta < 0 || abs(delta) < normalDelta)) {
            newBase = base + baselineDrift * (value - base)
            baseline = newBase
        }

        current = AirReading(status, raw, newBase.roundToInt(), 1f)
        return current
    }

    companion object {
        private val NUMBER = Regex("-?\\d+(\\.\\d+)?")

        /** Extrae el primer número de una línea, p. ej. "312", "MQ135:312" o "312.4". */
        fun parse(line: String): Int? =
            NUMBER.find(line)?.value?.toDoubleOrNull()?.roundToInt()
    }
}
