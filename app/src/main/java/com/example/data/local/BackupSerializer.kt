package com.example.data.local

import com.example.data.model.RouteEntity
import com.example.data.model.SegmentRecordEntity
import com.example.data.model.SegmentTemplateEntity
import com.example.data.model.TripRecordEntity
import org.json.JSONArray
import org.json.JSONObject

data class AstrolabioBackupData(
    val version: Int,
    val exportedAtMs: Long,
    val routes: List<RouteEntity>,
    val segmentTemplates: List<SegmentTemplateEntity>,
    val tripRecords: List<TripRecordEntity>,
    val segmentRecords: List<SegmentRecordEntity>
)

object BackupSerializer {
    private const val BACKUP_VERSION = 1

    fun exportToJson(
        routes: List<RouteEntity>,
        segmentTemplates: List<SegmentTemplateEntity>,
        tripRecords: List<TripRecordEntity>,
        segmentRecords: List<SegmentRecordEntity>,
        exportedAtMs: Long = System.currentTimeMillis()
    ): String {
        val root = JSONObject().apply {
            put("app", "Estroncio")
            put("version", BACKUP_VERSION)
            put("exportedAtMs", exportedAtMs)
        }

        val routesArray = JSONArray()
        routes.forEach { r ->
            routesArray.put(
                JSONObject().apply {
                    put("id", r.id)
                    put("name", r.name)
                    put("usageCount", r.usageCount)
                    put("createdAtMs", r.createdAtMs)
                    put("lastUsedAtMs", r.lastUsedAtMs)
                }
            )
        }
        root.put("routes", routesArray)

        val templatesArray = JSONArray()
        segmentTemplates.forEach { t ->
            templatesArray.put(
                JSONObject().apply {
                    put("id", t.id)
                    put("routeId", t.routeId)
                    put("orderIndex", t.orderIndex)
                    put("name", t.name)
                }
            )
        }
        root.put("segmentTemplates", templatesArray)

        val tripsArray = JSONArray()
        tripRecords.forEach { tr ->
            tripsArray.put(
                JSONObject().apply {
                    put("id", tr.id)
                    put("routeId", tr.routeId)
                    put("direction", tr.direction)
                    put("departureTimestampMs", tr.departureTimestampMs)
                    put("arrivalTimestampMs", tr.arrivalTimestampMs)
                    put("totalDurationSec", tr.totalDurationSec)
                    put("minuteOfDay", tr.minuteOfDay)
                    put("dayOfWeek", tr.dayOfWeek)
                    put("month", tr.month)
                    put("hasRain", tr.hasRain)
                    put("hasProtest", tr.hasProtest)
                    put("hasAccident", tr.hasAccident)
                    put("isVacation", tr.isVacation)
                    put("hasRide", tr.hasRide)
                    put("customIncident", tr.customIncident)
                }
            )
        }
        root.put("tripRecords", tripsArray)

        val segmentsArray = JSONArray()
        segmentRecords.forEach { sr ->
            segmentsArray.put(
                JSONObject().apply {
                    put("id", sr.id)
                    put("tripId", sr.tripId)
                    put("routeId", sr.routeId)
                    put("segmentOrder", sr.segmentOrder)
                    put("segmentName", sr.segmentName)
                    put("durationSec", sr.durationSec)
                }
            )
        }
        root.put("segmentRecords", segmentsArray)

        return root.toString(2)
    }

    fun parseFromJson(jsonText: String): AstrolabioBackupData {
        val root = JSONObject(jsonText)
        val version = root.optInt("version", 1)
        val exportedAtMs = root.optLong("exportedAtMs", System.currentTimeMillis())

        val routesArr = root.getJSONArray("routes")
        val routes = buildList {
            for (i in 0 until routesArr.length()) {
                val o = routesArr.getJSONObject(i)
                add(
                    RouteEntity(
                        id = o.getLong("id"),
                        name = o.getString("name"),
                        usageCount = o.optInt("usageCount", 0),
                        createdAtMs = o.optLong("createdAtMs", exportedAtMs),
                        lastUsedAtMs = o.optLong("lastUsedAtMs", exportedAtMs)
                    )
                )
            }
        }

        val templatesArr = root.optJSONArray("segmentTemplates") ?: JSONArray()
        val templates = buildList {
            for (i in 0 until templatesArr.length()) {
                val o = templatesArr.getJSONObject(i)
                add(
                    SegmentTemplateEntity(
                        id = o.optLong("id", 0L),
                        routeId = o.getLong("routeId"),
                        orderIndex = o.getInt("orderIndex"),
                        name = o.getString("name")
                    )
                )
            }
        }

        val tripsArr = root.optJSONArray("tripRecords") ?: JSONArray()
        val trips = buildList {
            for (i in 0 until tripsArr.length()) {
                val o = tripsArr.getJSONObject(i)
                add(
                    TripRecordEntity(
                        id = o.getLong("id"),
                        routeId = o.getLong("routeId"),
                        direction = o.optString("direction", "IDA"),
                        departureTimestampMs = o.getLong("departureTimestampMs"),
                        arrivalTimestampMs = o.getLong("arrivalTimestampMs"),
                        totalDurationSec = o.getLong("totalDurationSec"),
                        minuteOfDay = o.getInt("minuteOfDay"),
                        dayOfWeek = o.getInt("dayOfWeek"),
                        month = o.getInt("month"),
                        hasRain = o.optBoolean("hasRain", false),
                        hasProtest = o.optBoolean("hasProtest", false),
                        hasAccident = o.optBoolean("hasAccident", false),
                        isVacation = o.optBoolean("isVacation", false),
                        hasRide = o.optBoolean("hasRide", false),
                        customIncident = o.optString("customIncident", "")
                    )
                )
            }
        }

        val segmentsArr = root.optJSONArray("segmentRecords") ?: JSONArray()
        val segments = buildList {
            for (i in 0 until segmentsArr.length()) {
                val o = segmentsArr.getJSONObject(i)
                add(
                    SegmentRecordEntity(
                        id = o.optLong("id", 0L),
                        tripId = o.getLong("tripId"),
                        routeId = o.getLong("routeId"),
                        segmentOrder = o.getInt("segmentOrder"),
                        segmentName = o.getString("segmentName"),
                        durationSec = o.getLong("durationSec")
                    )
                )
            }
        }

        return AstrolabioBackupData(
            version = version,
            exportedAtMs = exportedAtMs,
            routes = routes,
            segmentTemplates = templates,
            tripRecords = trips,
            segmentRecords = segments
        )
    }
}
