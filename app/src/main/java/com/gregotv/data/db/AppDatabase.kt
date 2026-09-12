package com.gregotv.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        FavoriteEntity::class,
        ProgressEntity::class,
        ChannelHealthEntity::class
    ],
    version = 3,
    // Room writes the canonical CREATE TABLE for every version to
    // app/schemas. Without it there is nothing to check a hand-written
    // migration against, and nothing for MigrationTestHelper to build an old
    // database from.
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun progressDao(): ProgressDao
    abstract fun channelHealthDao(): ChannelHealthDao
}
