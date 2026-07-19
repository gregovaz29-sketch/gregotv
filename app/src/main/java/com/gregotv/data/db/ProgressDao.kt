package com.gregotv.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {
    @Query("SELECT * FROM progress ORDER BY updatedAt DESC LIMIT 20")
    fun observeRecent(): Flow<List<ProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ProgressEntity)

    @Query("SELECT positionMs FROM progress WHERE id = :id")
    suspend fun getPosition(id: String): Long?

    @Query("DELETE FROM progress WHERE id = :id")
    suspend fun clear(id: String)
}
