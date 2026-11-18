package com.example

import com.example.data.model.CircumstanceContext
import com.example.ui.SimulationParams
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class SimulationParamsTest {

    @Test
    fun `fromNow defaults to present moment`() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 17, 20, 35, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val fixedMs = cal.timeInMillis

        val params = SimulationParams.fromNow(fixedMs)
        val ctx = params.toCircumstanceContext(fixedMs)

        assertTrue(params.useCurrentTime)
        assertEquals(20, params.effectiveHour(fixedMs))
        assertEquals(35, params.effectiveMinute(fixedMs))
        assertEquals(2, params.effectiveDayOfWeek(fixedMs)) // Tuesday
        assertEquals(11, params.effectiveMonth(fixedMs)) // November
        assertEquals(fixedMs, params.departureTimeMs(fixedMs))
        assertEquals(CircumstanceContext.now(timestampMs = fixedMs), ctx)
    }

    @Test
    fun `custom simulation updates departure timestamp and circumstance context`() {
        val baseCal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 17, 20, 35, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val fixedMs = baseCal.timeInMillis

        val custom = SimulationParams.fromNow(fixedMs)
            .withBaseSyncedToNow(fixedMs)
            .copy(
                useCurrentTime = false,
                customHour = 7,
                customMinute = 45,
                customDayOfWeek = 1, // Monday
                customMonth = 7, // July
                hasRain = true,
                hasRide = true
            )

        assertFalse(custom.useCurrentTime)
        val depCal = Calendar.getInstance().apply {
            timeInMillis = custom.departureTimeMs(fixedMs)
        }
        assertEquals(Calendar.JULY, depCal.get(Calendar.MONTH))
        assertEquals(Calendar.MONDAY, depCal.get(Calendar.DAY_OF_WEEK))
        assertEquals(7, depCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(45, depCal.get(Calendar.MINUTE))

        val ctx = custom.toCircumstanceContext(fixedMs)
        assertEquals(7 * 60 + 45, ctx.minuteOfDay)
        assertEquals(1, ctx.dayOfWeek)
        assertEquals(7, ctx.month)
        assertTrue(ctx.hasRain)
        assertTrue(ctx.hasRide)
    }
}
