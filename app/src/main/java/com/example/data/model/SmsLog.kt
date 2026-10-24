package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sms_logs")
data class SmsLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long? = null,
    val customerName: String = "",
    val recipientPhone: String,
    val messageText: String,
    val status: String = "SENT", // SENT or FAILED
    val sentAt: Long = System.currentTimeMillis()
)

data class AppSettings(
    val storeName: String = "مدير الديون الذكي",
    val ownerPhone: String = "",
    val currency: String = "ر.س",
    val defaultSmsTemplate: String = "عزيزنا {name}، نود تذكيركم بأن رصيد حسابكم المستحق هو {balance} {currency}. يرجى التكرم بالسداد في أقرب وقت شاكرين لكم حسن تعاونكم.",
    val autoSendSmsOnDebt: Boolean = false
)
