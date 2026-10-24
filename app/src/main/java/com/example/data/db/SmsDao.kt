package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.SmsLog
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsDao {
    @Query("SELECT * FROM sms_logs ORDER BY sentAt DESC")
    fun getAllLogs(): Flow<List<SmsLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: SmsLog): Long

    @Query("DELETE FROM sms_logs WHERE id = :id")
    suspend fun deleteLogById(id: Long)

    @Query("DELETE FROM sms_logs")
    suspend fun clearAll()
}
