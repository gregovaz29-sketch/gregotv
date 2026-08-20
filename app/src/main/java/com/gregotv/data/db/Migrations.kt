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

val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2)
