package com.pranay.jobtracker.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [JobApplication::class, SyncMetadata::class, EmailEvent::class, AccountInfo::class],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun applicationDao(): ApplicationDao
    abstract fun syncMetadataDao(): SyncMetadataDao
    abstract fun emailEventDao(): EmailEventDao
    abstract fun accountInfoDao(): AccountInfoDao

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
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. Create accounts table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS accounts (
                        accountId TEXT NOT NULL PRIMARY KEY,
                        email TEXT NOT NULL,
                        displayName TEXT NOT NULL,
                        photoUrl TEXT NOT NULL,
                        colorHash TEXT NOT NULL,
                        isActive INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())

                // 2. Add accountId to applications with default
                database.execSQL("ALTER TABLE applications ADD COLUMN accountId TEXT NOT NULL DEFAULT 'legacy_account'")

                // 3. Add accountId to email_events with default
                database.execSQL("ALTER TABLE email_events ADD COLUMN accountId TEXT NOT NULL DEFAULT 'legacy_account'")

                // 4. Rebuild sync_metadata to change Primary Key to accountId and add lastHistoryId
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS sync_metadata_new (
                        accountId TEXT NOT NULL PRIMARY KEY,
                        oldestFetchedEpochMs INTEGER NOT NULL,
                        lastHistoryId TEXT
                    )
                """.trimIndent())
                
                // Copy existing meta (if any) to the legacy_account slot
                database.execSQL("""
                    INSERT INTO sync_metadata_new (accountId, oldestFetchedEpochMs, lastHistoryId)
                    SELECT 'legacy_account', oldestFetchedEpochMs, NULL FROM sync_metadata
                """.trimIndent())

                database.execSQL("DROP TABLE sync_metadata")
                database.execSQL("ALTER TABLE sync_metadata_new RENAME TO sync_metadata")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add Multi-Stage tracking columns
                database.execSQL("ALTER TABLE applications ADD COLUMN stage TEXT NOT NULL DEFAULT 'APPLIED'")
                database.execSQL("ALTER TABLE applications ADD COLUMN subStatus TEXT")
                
                // Smart Auto-Mapping execution (Option B) 
                // Uses broad case-insensitive constraints to identify deep status traits, converting up structure
                database.execSQL("UPDATE applications SET stage = 'INTERVIEW' WHERE LOWER(status) LIKE '%interview%' OR LOWER(status) LIKE '%round%'")
                database.execSQL("UPDATE applications SET stage = 'OFFER' WHERE LOWER(status) LIKE '%offer%' OR LOWER(status) LIKE '%hired%'")
                database.execSQL("UPDATE applications SET stage = 'REJECTED' WHERE LOWER(status) LIKE '%reject%' OR LOWER(status) LIKE '%not selected%'")
                database.execSQL("UPDATE applications SET stage = 'ASSESSMENT' WHERE LOWER(status) LIKE '%assessment%' OR LOWER(status) LIKE '%oa%' OR LOWER(status) LIKE '%test%' OR LOWER(status) LIKE '%assignment%'")
                database.execSQL("UPDATE applications SET stage = 'WITHDRAWN' WHERE LOWER(status) LIKE '%withdraw%'")
                database.execSQL("UPDATE applications SET stage = 'IN_REVIEW' WHERE LOWER(status) LIKE '%review%' OR LOWER(status) LIKE '%process%' OR LOWER(status) LIKE '%consideration%'")
                
                // Redundancy: Map complete original strings natively to SubStatus to permanently archive specifics
                database.execSQL("UPDATE applications SET subStatus = status")
            }
        }
    }
}
