package com.example.data.model

import java.util.Calendar

enum class TripDirection(val labelEs: String, val shortLabel: String) {
    IDA("Ida", "IDA"),
    VUELTA("Vuelta", "VUELTA");

    companion object {
        fun fromString(raw: String): TripDirection =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: IDA
    }
}

enum class IncidentType(val labelEs: String, val astronomicalMetaphor: String) {
    LLUVIA("Lluvia", "Nebulosa pluvial"),
    MANIFESTACION("Manifestación", "Eclipse vial"),
    ACCIDENTE("Accidente", "Perturbación orbital"),
    VACACIONES("Vacaciones", "Solsticio calmo"),
    AVENTON("Aventón", "Impulso gravitacional")
}

data class CircumstanceContext(
    val direction: TripDirection,
    val hasRain: Boolean = false,
    val hasProtest: Boolean = false,
    val hasAccident: Boolean = false,
    val isVacation: Boolean = false,
    val hasRide: Boolean = false,
    val customIncident: String = "",
    val minuteOfDay: Int,
    val dayOfWeek: Int, // 1 = Lunes .. 7 = Domingo (ISO)
    val month: Int      // 1 = Enero .. 12 = Diciembre
) {
    val hasAnyExtraordinary: Boolean
        get() = hasRain || hasProtest || hasAccident || isVacation || hasRide || customIncident.isNotBlank()

    fun summaryLabels(): List<String> = buildList {
        add(direction.labelEs)
        if (hasRain) add(IncidentType.LLUVIA.labelEs)
        if (hasProtest) add(IncidentType.MANIFESTACION.labelEs)
        if (hasAccident) add(IncidentType.ACCIDENTE.labelEs)
        if (isVacation) add(IncidentType.VACACIONES.labelEs)
        if (hasRide) add(IncidentType.AVENTON.labelEs)
        if (customIncident.isNotBlank()) add(customIncident.trim())
    }

    companion object {
        fun now(
            direction: TripDirection = TripDirection.IDA,
            hasRain: Boolean = false,
            hasProtest: Boolean = false,
            hasAccident: Boolean = false,
            isVacation: Boolean = false,
            hasRide: Boolean = false,
            customIncident: String = "",
            timestampMs: Long = System.currentTimeMillis()
        ): CircumstanceContext {
            val cal = Calendar.getInstance().apply { timeInMillis = timestampMs }
            val minute = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            val calDow = cal.get(Calendar.DAY_OF_WEEK) // Sun=1, Mon=2 .. Sat=7
            val isoDow = if (calDow == Calendar.SUNDAY) 7 else calDow - 1
            val month = cal.get(Calendar.MONTH) + 1
            return CircumstanceContext(
                direction = direction,
                hasRain = hasRain,
                hasProtest = hasProtest,
                hasAccident = hasAccident,
                isVacation = isVacation,
                hasRide = hasRide,
                customIncident = customIncident.trim(),
                minuteOfDay = minute,
                dayOfWeek = isoDow,
                month = month
            )
        }
    }
}
