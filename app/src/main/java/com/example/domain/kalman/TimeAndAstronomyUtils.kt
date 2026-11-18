package com.example.domain.kalman

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AstronomyEpigraph(
    val figure: String,
    val era: String,
    val note: String
)

object AstronomyHistory {
    val epigraphs = listOf(
        AstronomyEpigraph(
            figure = "Hiparco de Nicea",
            era = "c. 150 a. C.",
            note = "Sistematizó las coordenadas celestes y perfeccionó el astrolabio para predecir el tránsito de los astros."
        ),
        AstronomyEpigraph(
            figure = "Al-Biruni",
            era = "c. 1000 d. C.",
            note = "Calculó la altura de los cuerpos celestes y diseñó engranajes astronómicos para medir el tiempo con precisión."
        ),
        AstronomyEpigraph(
            figure = "Tycho Brahe",
            era = "Uraniborg, 1576",
            note = "Sus efemérides observacionales demostraron que registrar cada medición mejora la predicción orbital."
        ),
        AstronomyEpigraph(
            figure = "Johannes Kepler",
            era = "Praga, 1609",
            note = "Descubrió que el tiempo de recorrido varía a lo largo de la órbita: cada tramo tiene su propia cadencia."
        ),
        AstronomyEpigraph(
            figure = "C. F. Gauss & R. Kalman",
            era = "1801 → 1960",
            note = "Del cálculo de la órbita de Ceres al Filtro de Kalman: separar la señal real de las anomalías atípicas."
        ),
        AstronomyEpigraph(
            figure = "J. Fraunhofer & C. Payne",
            era = "1814 → 1925",
            note = "Las líneas espectrales —como el estroncio estelar (Sr II)— revelaron la composición y edad de los astros."
        )
    )

    fun epigraphForSeed(seed: Long): AstronomyEpigraph {
        val index = (kotlin.math.abs(seed) % epigraphs.size).toInt()
        return epigraphs[index]
    }
}

object TimeFormatters {
    private val clockFormat = SimpleDateFormat("h:mm a", Locale.US)
    private val dateTimeFormat = SimpleDateFormat("dd MMM · h:mm a", Locale("es", "MX"))

    fun formatClock(timestampMs: Long): String = clockFormat.format(Date(timestampMs))

    fun formatDateTime(timestampMs: Long): String = dateTimeFormat.format(Date(timestampMs))

    fun formatHourMinute12(hour24: Int, minute: Int): String {
        val h24 = hour24.coerceIn(0, 23)
        val min = minute.coerceIn(0, 59)
        val period = if (h24 < 12) "AM" else "PM"
        val h12 = when {
            h24 == 0 -> 12
            h24 > 12 -> h24 - 12
            else -> h24
        }
        return String.format(Locale.US, "%d:%02d %s", h12, min, period)
    }

    fun formatDurationHuman(seconds: Long): String {
        val s = seconds.coerceAtLeast(0L)
        val hours = s / 3600L
        val mins = (s % 3600L) / 60L
        val remSec = s % 60L
        return when {
            hours > 0 -> "${hours}h ${mins}m"
            mins > 0 -> "${mins} min"
            else -> "${remSec} s"
        }
    }

    fun formatChronometer(seconds: Long): String {
        val s = seconds.coerceAtLeast(0L)
        val hours = s / 3600L
        val mins = (s % 3600L) / 60L
        val secs = s % 60L
        return if (hours > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, mins, secs)
        } else {
            String.format(Locale.US, "%02d:%02d", mins, secs)
        }
    }

    fun dayOfWeekLabel(isoDow: Int): String = when (isoDow) {
        1 -> "Lunes"
        2 -> "Martes"
        3 -> "Miércoles"
        4 -> "Jueves"
        5 -> "Viernes"
        6 -> "Sábado"
        7 -> "Domingo"
        else -> "Día"
    }

    fun monthLabel(month: Int): String = when (month) {
        1 -> "Enero"
        2 -> "Febrero"
        3 -> "Marzo"
        4 -> "Abril"
        5 -> "Mayo"
        6 -> "Junio"
        7 -> "Julio"
        8 -> "Agosto"
        9 -> "Septiembre"
        10 -> "Octubre"
        11 -> "Noviembre"
        12 -> "Diciembre"
        else -> "Mes"
    }
}
