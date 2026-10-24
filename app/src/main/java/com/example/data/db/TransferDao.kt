package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Transfer
import kotlinx.coroutines.flow.Flow

@Dao
interface TransferDao {
    @Query("SELECT * FROM transfers ORDER BY date DESC, id DESC")
    fun getAllTransfers(): Flow<List<Transfer>>

    @Query("SELECT * FROM transfers WHERE status = :status ORDER BY date DESC")
    fun getTransfersByStatus(status: String): Flow<List<Transfer>>

    @Query("SELECT * FROM transfers WHERE id = :id LIMIT 1")
    suspend fun getTransferById(id: Long): Transfer?

    @Query("SELECT * FROM transfers WHERE transferNumber = :transferNumber LIMIT 1")
    suspend fun findByTransferNumber(transferNumber: String): Transfer?

    @Query("SELECT COUNT(*) > 0 FROM transfers WHERE transferNumber = :transferNumber")
    suspend fun existsByTransferNumber(transferNumber: String): Boolean

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTransfer(transfer: Transfer): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transfers: List<Transfer>)

    @Update
    suspend fun updateTransfer(transfer: Transfer)

    @Delete
    suspend fun deleteTransfer(transfer: Transfer)

    @Query("DELETE FROM transfers WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM transfers")
    suspend fun clearAll()
}
