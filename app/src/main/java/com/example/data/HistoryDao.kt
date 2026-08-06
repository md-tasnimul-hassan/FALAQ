package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<History>>

    @Insert
    suspend fun insert(history: History)

    @Delete
    suspend fun delete(history: History)

    @Query("DELETE FROM history")
    suspend fun clearHistory()
}
