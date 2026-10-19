package za.co.giantautoupholstery.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ProfileEntity::class,
        ServiceEntity::class,
        BookingEntity::class,
        NotificationEntity::class,
        FaqEntity::class,
        ContactMessageEntity::class,
        SyncLogEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class GiantDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun serviceDao(): ServiceDao
    abstract fun bookingDao(): BookingDao
    abstract fun notificationDao(): NotificationDao
    abstract fun faqDao(): FaqDao
    abstract fun contactDao(): ContactDao
    abstract fun fixtureDao(): FixtureDao
    abstract fun syncLogDao(): SyncLogDao

    companion object {
        const val NAME = "giant_auto_upholstery.db"
    }
}

/**
 * Version 2 introduces api_sync_logs relational table for tracking API synchronization events.
 * NEVER use fallbackToDestructiveMigration(): every schema change has an explicit migration.
 */
object Migrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS api_sync_logs (
                    id TEXT NOT NULL PRIMARY KEY,
                    endpoint TEXT NOT NULL,
                    httpMethod TEXT NOT NULL,
                    statusCode INTEGER NOT NULL,
                    statusText TEXT NOT NULL,
                    requestPayload TEXT,
                    responsePayload TEXT,
                    durationMs INTEGER NOT NULL,
                    success INTEGER NOT NULL,
                    errorMessage TEXT,
                    timestamp INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_api_sync_logs_timestamp ON api_sync_logs(timestamp)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_api_sync_logs_endpoint ON api_sync_logs(endpoint)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_api_sync_logs_statusCode ON api_sync_logs(statusCode)")
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2)
}
