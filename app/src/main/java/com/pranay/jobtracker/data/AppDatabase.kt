package com.pranay.jobtracker.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [JobApplication::class, SyncMetadata::class, EmailEvent::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun applicationDao(): ApplicationDao
    abstract fun syncMetadataDao(): SyncMetadataDao
    abstract fun emailEventDao(): EmailEventDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS sync_metadata " +
                    "(id INTEGER NOT NULL PRIMARY KEY, oldestFetchedEpochMs INTEGER NOT NULL DEFAULT 0)"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE applications ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE applications ADD COLUMN lastUpdatedAt INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE applications ADD COLUMN summary TEXT")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_applications_companyName ON applications(companyName)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_applications_jobTitle ON applications(jobTitle)")
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS email_events (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        jobApplicationId INTEGER NOT NULL,
                        gmailMessageId TEXT NOT NULL,
                        date TEXT NOT NULL,
                        dateEpochMs INTEGER NOT NULL,
                        snippet TEXT NOT NULL,
                        detectedStatus TEXT NOT NULL,
                        summary TEXT,
                        FOREIGN KEY (jobApplicationId) REFERENCES applications(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_email_events_gmailMessageId ON email_events(gmailMessageId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_email_events_jobApplicationId ON email_events(jobApplicationId)")
            }
        }
    }
}
