package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["customerId"])]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val type: String, // "DEBT" or "PAYMENT"
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val category: String = "عام", // بضاعة، فاتورة، خدمة، نقد، حوالة
    val invoiceNumber: String = "",
    val paymentMethod: String = "نقداً", // نقداً، حوالة مصرفية، شيك، تحويل
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
