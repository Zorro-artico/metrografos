package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.ActiveSegmentMarkEntity
import com.example.data.model.ActiveTripEntity
import com.example.data.model.RouteEntity
import com.example.data.model.SegmentRecordEntity
import com.example.data.model.SegmentTemplateEntity
import com.example.data.model.SuspendedSegmentMarkEntity
import com.example.data.model.SuspendedTripEntity
import com.example.data.model.TripRecordEntity

@Database(
    entities = [
        RouteEntity::class,
        SegmentTemplateEntity::class,
        TripRecordEntity::class,
        SegmentRecordEntity::class,
        ActiveTripEntity::class,
        ActiveSegmentMarkEntity::class,
        SuspendedTripEntity::class,
        SuspendedSegmentMarkEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AstrolabioDatabase : RoomDatabase() {
    abstract fun astrolabioDao(): AstrolabioDao

    companion object {
        @Volatile
        private var INSTANCE: AstrolabioDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trip_records ADD COLUMN hasRide INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE active_trip ADD COLUMN hasRide INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS suspended_trip (
                        id INTEGER NOT NULL PRIMARY KEY,
                        wasCompleted INTEGER NOT NULL,
                        savedTripRecordId INTEGER,
                        routeId INTEGER NOT NULL,
                        routeName TEXT NOT NULL,
                        direction TEXT NOT NULL,
                        departureTimestampMs INTEGER NOT NULL,
                        lastMarkTimestampMs INTEGER NOT NULL,
                        currentSegmentIndex INTEGER NOT NULL,
                        predictedTotalSec INTEGER NOT NULL,
                        predictedArrivalTimestampMs INTEGER NOT NULL,
                        hasRain INTEGER NOT NULL,
                        hasProtest INTEGER NOT NULL,
                        hasAccident INTEGER NOT NULL,
                        isVacation INTEGER NOT NULL,
                        hasRide INTEGER NOT NULL,
                        customIncident TEXT NOT NULL,
                        suspendedAtMs INTEGER NOT NULL,
                        expiresAtMs INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS suspended_segment_marks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        segmentOrder INTEGER NOT NULL,
                        segmentName TEXT NOT NULL,
                        durationSec INTEGER NOT NULL,
                        markedAtMs INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): AstrolabioDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AstrolabioDatabase::class.java,
                    "astrolabio_local.db"
                ).addMigrations(MIGRATION_1_2).build().also { INSTANCE = it }
            }
        }
    }
}
