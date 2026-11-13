package com.cleansense.app.sensor

import com.cleansense.app.bluetooth.DeviceItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Sensor simulado para probar la app en el emulador (que no tiene Bluetooth real).
 * Solo aparece en las compilaciones de depuración (debug).
 *
 * Envía una lectura cada 500 ms, igual que el Arduino, recorriendo un ciclo de 35 s:
 *   0-10 s   aire normal (incluye la calibración)
 *   10-13 s  aparece un olor (sube la lectura)
 *   13-19 s  olor fuerte          → "Mandar a lavar"
 *   19-22 s  el olor baja rápido
 *   22-30 s  baja lentamente      → "Volviendo a niveles base"
 *   30-35 s  aire normal otra vez → "aire normal"
 */
object DemoSensor {

    val DEVICE = DeviceItem(
        name = "CleanSense Demo",
        address = "DEMO",
        paired = true,
        isDemo = true,
    )

    private const val INTERVAL_MS = 500L
    private const val BASE = 180.0
    private const val PEAK = 330.0
    private const val CYCLE = 70

    fun lines(): Flow<String> = flow {
        var sample = 0
        while (true) {
            val noise = Random.nextInt(-4, 5)
            emit((valueAt(sample % CYCLE) + noise).roundToInt().toString())
            sample++
            delay(INTERVAL_MS)
        }
    }

    private fun valueAt(i: Int): Double = when (i) {
        in 0..19 -> BASE
        in 20..25 -> lerp(BASE, PEAK, (i - 20) / 5.0)
        in 26..37 -> PEAK
        in 38..43 -> lerp(PEAK, BASE + 40, (i - 38) / 5.0)
        in 44..59 -> lerp(BASE + 40, BASE + 10, (i - 44) / 15.0)
        else -> BASE
    }

    private fun lerp(from: Double, to: Double, t: Double) = from + (to - from) * t
}
