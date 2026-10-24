package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val address: String = "",
    val notes: String = "",
    val creditLimit: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

data class CustomerWithBalance(
    val customer: Customer,
    val totalDebt: Double = 0.0,
    val totalPaid: Double = 0.0,
    val currentBalance: Double = 0.0, // currentBalance = totalDebt - totalPaid
    val transactionCount: Int = 0,
    val lastTransactionDate: Long? = null
)
