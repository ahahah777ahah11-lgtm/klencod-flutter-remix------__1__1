package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transfers",
    indices = [Index(value = ["transferNumber"], unique = true)]
)
data class Transfer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val companyName: String, // الكريمي، النجم، ويسترن يونيون، الراجحي، إلخ
    val transferNumber: String,
    val amount: Double,
    val senderName: String,
    val customerId: Long? = null,
    val date: Long = System.currentTimeMillis(),
    val status: String = "PENDING", // PENDING or CONFIRMED
    val note: String = "",
    val confirmedTransactionId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
