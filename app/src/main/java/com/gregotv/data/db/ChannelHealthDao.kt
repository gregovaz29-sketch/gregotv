package com.gregotv.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ChannelHealthDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: ChannelHealthEntity)

    /** URLs marked dead within the quarantine window; used by the loader. */
    @Query(
        "SELECT url FROM channel_health " +
            "WHERE status = 'dead' AND lastCheckedAt >= :since"
    )
    suspend fun deadSince(since: Long): List<String>

    /** House-keeping: forget records older than the quarantine window. */
    @Query("DELETE FROM channel_health WHERE lastCheckedAt < :cutoff")
    suspend fun purgeOlderThan(cutoff: Long)

    companion object {
        /** Three days: long enough to skip stale channels, short enough to retry. */
        const val QUARANTINE_MS: Long = 3L * 24L * 60L * 60L * 1000L
    }
}
