package com.gregotv.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

private const val TEST_DB = "migration-test.db"

/**
 * The v1 -> v2 migration is the one change in this app that can destroy the
 * user's favorites and watch progress, and it cannot be verified by compiling.
 * This runs it against real SQLite on a real device.
 *
 * It builds the v1 database by hand rather than with
 * `MigrationTestHelper.createDatabase`: `exportSchema` was off when v1 shipped,
 * so there is no `1.json` to build one from. From v2 onwards the schemas are
 * exported, so later migrations can use the helper normally.
 *
 * The strongest assertion is the setup itself: the migrated file is reopened
 * through the real [AppDatabase]. If [ChannelHealthEntity] disagreed with the
 * migration's CREATE TABLE by so much as a column type, Room would throw
 * IllegalStateException right there — the exact crash this guards against.
 */
@RunWith(AndroidJUnit4::class)
class Migration1To2Test {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    /** Verbatim from the v1 entities, before channel_health existed. */
    private val v1Schema = listOf(
        "CREATE TABLE IF NOT EXISTS `favorites` (" +
            "`id` TEXT NOT NULL, `title` TEXT NOT NULL, `url` TEXT NOT NULL, " +
            "`type` TEXT NOT NULL, `posterUrl` TEXT, `group` TEXT, " +
            "PRIMARY KEY(`id`))",
        "CREATE TABLE IF NOT EXISTS `progress` (" +
            "`id` TEXT NOT NULL, `title` TEXT NOT NULL, `url` TEXT NOT NULL, " +
            "`type` TEXT NOT NULL, `posterUrl` TEXT, `positionMs` INTEGER NOT NULL, " +
            "`durationMs` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, " +
            "PRIMARY KEY(`id`))"
    )

    @Test
    fun favoritesAndProgressSurviveTheMigration() {
        context.deleteDatabase(TEST_DB)

        // 1. A v1 database with the user's data actually in it. Room stamps
        //    its own version, so set it the way v1 would have left it.
        context.openOrCreateDatabase(TEST_DB, Context.MODE_PRIVATE, null).use { v1 ->
            v1Schema.forEach(v1::execSQL)
            v1.execSQL(
                "INSERT INTO favorites (id,title,url,type,posterUrl,`group`) " +
                    "VALUES ('fav1','La 1','http://example/la1.m3u8','LIVE_CHANNEL',NULL,'España')"
            )
            v1.execSQL(
                "INSERT INTO progress " +
                    "(id,title,url,type,posterUrl,positionMs,durationMs,updatedAt) " +
                    "VALUES ('prog1','Una peli','http://example/peli.mp4','MOVIE'," +
                    "NULL,90000,7200000,1755000000000)"
            )
            v1.execSQL(
                "CREATE TABLE IF NOT EXISTS room_master_table " +
                    "(id INTEGER PRIMARY KEY, identity_hash TEXT)"
            )
            v1.version = 1
        }

        // 2. Migrate by opening the real database exactly as the app does.
        val db = Room.databaseBuilder(context, AppDatabase::class.java, TEST_DB)
            .addMigrations(*ALL_MIGRATIONS)
            .build()

        try {
            // 3. The user's data is still there.
            val favorites = runBlocking { db.favoriteDao().observeAll().first() }
            assertEquals(1, favorites.size)
            assertEquals("La 1", favorites.single().title)
            assertEquals("España", favorites.single().group)

            val position: Long = runBlocking { db.progressDao().getPosition("prog1") } ?: -1L
            assertEquals(90_000L, position)

            // 4. And the new table is usable, which means the entity matches
            //    the CREATE TABLE the migration ran.
            runBlocking {
                db.channelHealthDao().upsert(
                    ChannelHealthEntity(
                        url = "http://example/dead.m3u8",
                        status = "dead",
                        lastCheckedAt = 1_755_000_000_000
                    )
                )
                assertTrue("http://example/dead.m3u8" in db.channelHealthDao().deadSince(0))
            }
        } finally {
            db.close()
            context.deleteDatabase(TEST_DB)
        }
    }
}
