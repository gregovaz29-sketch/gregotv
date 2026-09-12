package com.gregotv.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Real migrations. `fallbackToDestructiveMigration()` used to be a fallback
 * safety net, but any schema bump under it silently drops the user's favorites
 * and watch progress. Every new version must add its Migration here.
 */

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `channel_health` (
                `url` TEXT NOT NULL,
                `status` TEXT NOT NULL,
                `lastCheckedAt` INTEGER NOT NULL,
                PRIMARY KEY(`url`)
            )
            """.trimIndent()
        )
    }
}

/**
 * v3 keeps local history instead of hiding a channel after its first error.
 * One timeout can be a temporary server hiccup; three separate failures is a
 * much better signal, and a successful ten-second playback resets the count.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE `channel_health` ADD COLUMN `failureCount` INTEGER NOT NULL DEFAULT 0"
        )
    }
}

val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
