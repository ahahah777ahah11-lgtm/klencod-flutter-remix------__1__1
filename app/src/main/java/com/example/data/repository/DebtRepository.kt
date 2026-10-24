package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.db.AppDatabase
import com.example.data.model.AppSettings
import com.example.data.model.Customer
import com.example.data.model.CustomerWithBalance
import com.example.data.model.SmsLog
import com.example.data.model.Transaction
import com.example.data.model.Transfer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DebtRepository(
    private val database: AppDatabase,
    private val context: Context
) {
    private val customerDao = database.customerDao()
    private val transactionDao = database.transactionDao()
    private val transferDao = database.transferDao()
    private val smsDao = database.smsDao()

    private val prefs: SharedPreferences =
        context.getSharedPreferences("smart_debt_settings", Context.MODE_PRIVATE)

    // Flow of all customers paired with their real-time calculated balances
    val customersWithBalances: Flow<List<CustomerWithBalance>> =
        combine(
            customerDao.getAllCustomers(),
            transactionDao.getAllTransactions()
        ) { customers, transactions ->
            val transactionsByCustomer = transactions.groupBy { it.customerId }
            customers.map { customer ->
                val customerTxns = transactionsByCustomer[customer.id] ?: emptyList()
                val totalDebt = customerTxns.filter { it.type == "DEBT" }.sumOf { it.amount }
                val totalPaid = customerTxns.filter { it.type == "PAYMENT" }.sumOf { it.amount }
                val balance = totalDebt - totalPaid
                val latestTxnDate = customerTxns.maxByOrNull { it.date }?.date

                CustomerWithBalance(
                    customer = customer,
                    totalDebt = totalDebt,
                    totalPaid = totalPaid,
                    currentBalance = balance,
                    transactionCount = customerTxns.size,
                    lastTransactionDate = latestTxnDate
                )
            }.sortedByDescending { it.currentBalance }
        }

    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()
    val recentTransactions: Flow<List<Transaction>> = transactionDao.getRecentTransactions(10)
    val totalDebts: Flow<Double> = transactionDao.getTotalDebts()
    val totalPayments: Flow<Double> = transactionDao.getTotalPayments()
    val customerCount: Flow<Int> = customerDao.getCustomerCount()
    val allTransfers: Flow<List<Transfer>> = transferDao.getAllTransfers()
    val smsLogs: Flow<List<SmsLog>> = smsDao.getAllLogs()

    fun getTransactionsForCustomer(customerId: Long): Flow<List<Transaction>> =
        transactionDao.getTransactionsForCustomer(customerId)

    suspend fun getCustomerById(id: Long): Customer? = customerDao.getCustomerById(id)

    suspend fun insertCustomer(customer: Customer): Long = withContext(Dispatchers.IO) {
        customerDao.insertCustomer(customer)
    }

    suspend fun updateCustomer(customer: Customer) = withContext(Dispatchers.IO) {
        customerDao.updateCustomer(customer)
    }

    suspend fun deleteCustomer(customer: Customer) = withContext(Dispatchers.IO) {
        customerDao.deleteCustomer(customer)
    }

    suspend fun recordDebt(
        customerId: Long,
        amount: Double,
        category: String,
        invoiceNumber: String,
        date: Long,
        note: String
    ): Long = withContext(Dispatchers.IO) {
        val txn = Transaction(
            customerId = customerId,
            type = "DEBT",
            amount = amount,
            category = category.ifBlank { "دين بضاعة" },
            invoiceNumber = invoiceNumber,
            date = date,
            paymentMethod = "-",
            note = note
        )
        transactionDao.insertTransaction(txn)
    }

    suspend fun recordPayment(
        customerId: Long,
        amount: Double,
        paymentMethod: String,
        receiptNumber: String,
        date: Long,
        note: String
    ): Long = withContext(Dispatchers.IO) {
        val txn = Transaction(
            customerId = customerId,
            type = "PAYMENT",
            amount = amount,
            category = "سداد دفعة",
            invoiceNumber = receiptNumber,
            date = date,
            paymentMethod = paymentMethod.ifBlank { "نقداً" },
            note = note
        )
        transactionDao.insertTransaction(txn)
    }

    suspend fun deleteTransaction(transaction: Transaction) = withContext(Dispatchers.IO) {
        transactionDao.deleteTransaction(transaction)
    }

    // Transfers
    suspend fun recordTransfer(transfer: Transfer): Result<Long> = withContext(Dispatchers.IO) {
        val cleanNumber = transfer.transferNumber.trim()
        if (cleanNumber.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("رقم الحوالة لا يمكن أن يكون فارغاً"))
        }
        val exists = transferDao.existsByTransferNumber(cleanNumber)
        if (exists) {
            return@withContext Result.failure(IllegalStateException("رقم الحوالة ($cleanNumber) مسجل مسبقاً لمنع التكرار!"))
        }
        val id = transferDao.insertTransfer(transfer.copy(transferNumber = cleanNumber))
        Result.success(id)
    }

    suspend fun confirmTransfer(transferId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val transfer = transferDao.getTransferById(transferId)
            ?: return@withContext Result.failure(Exception("الحوالة غير موجودة"))

        if (transfer.status == "CONFIRMED") {
            return@withContext Result.failure(Exception("الحوالة مؤكدة مسبقاً"))
        }

        val custId = transfer.customerId
            ?: return@withContext Result.failure(Exception("يجب ربط الحوالة بعميل قبل تأكيد الاستلام"))

        // Create transaction as payment
        val txnId = transactionDao.insertTransaction(
            Transaction(
                customerId = custId,
                type = "PAYMENT",
                amount = transfer.amount,
                category = "حوالة واردة",
                invoiceNumber = transfer.transferNumber,
                paymentMethod = transfer.companyName,
                note = "حوالة رقم: ${transfer.transferNumber} عن طريق ${transfer.companyName} المرسل: ${transfer.senderName}",
                date = transfer.date
            )
        )

        // Update transfer status
        transferDao.updateTransfer(
            transfer.copy(
                status = "CONFIRMED",
                confirmedTransactionId = txnId
            )
        )
        Result.success(Unit)
    }

    suspend fun deleteTransfer(transfer: Transfer) = withContext(Dispatchers.IO) {
        transferDao.deleteTransfer(transfer)
    }

    // SMS Log
    suspend fun logSms(log: SmsLog) = withContext(Dispatchers.IO) {
        smsDao.insertLog(log)
    }

    // App Settings
    fun getSettings(): AppSettings {
        return AppSettings(
            storeName = prefs.getString("store_name", "مدير الديون الذكي") ?: "مدير الديون الذكي",
            ownerPhone = prefs.getString("owner_phone", "") ?: "",
            currency = prefs.getString("currency", "ر.س") ?: "ر.س",
            defaultSmsTemplate = prefs.getString(
                "sms_template",
                "عزيزنا {name}، رصيد حسابكم الحالي هو {balance} {currency}. يرجى التكرم بالسداد في أقرب وقت. شاكرين لكم حسن تعاملكم."
            ) ?: "",
            autoSendSmsOnDebt = prefs.getBoolean("auto_sms", false)
        )
    }

    fun saveSettings(settings: AppSettings) {
        prefs.edit()
            .putString("store_name", settings.storeName)
            .putString("owner_phone", settings.ownerPhone)
            .putString("currency", settings.currency)
            .putString("sms_template", settings.defaultSmsTemplate)
            .putBoolean("auto_sms", settings.autoSendSmsOnDebt)
            .apply()
    }

    // Template rendering
    fun renderSmsMessage(template: String, customerName: String, balance: Double, currency: String): String {
        val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        val balanceFormatted = "%,.2f".format(Locale.US, balance)
        val store = getSettings().storeName

        return template
            .replace("{name}", customerName)
            .replace("{اسم_العميل}", customerName)
            .replace("{balance}", balanceFormatted)
            .replace("{المبلغ_المستحق}", balanceFormatted)
            .replace("{currency}", currency)
            .replace("{العملة}", currency)
            .replace("{date}", currentDate)
            .replace("{تاريخ_اليوم}", currentDate)
            .replace("{store}", store)
            .replace("{اسم_المحل}", store)
    }

    // Export to JSON string
    suspend fun exportDataAsJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        val customers = customerDao.getAllCustomers()
        // Synchronous read for export
        // Query database directly
        val dbCustomers = database.openHelper.readableDatabase.query("SELECT * FROM customers")
        val custList = JSONArray()
        while (dbCustomers.moveToNext()) {
            val c = JSONObject().apply {
                put("id", dbCustomers.getLong(dbCustomers.getColumnIndexOrThrow("id")))
                put("name", dbCustomers.getString(dbCustomers.getColumnIndexOrThrow("name")))
                put("phone", dbCustomers.getString(dbCustomers.getColumnIndexOrThrow("phone")))
                put("address", dbCustomers.getString(dbCustomers.getColumnIndexOrThrow("address")))
                put("notes", dbCustomers.getString(dbCustomers.getColumnIndexOrThrow("notes")))
                put("creditLimit", dbCustomers.getDouble(dbCustomers.getColumnIndexOrThrow("creditLimit")))
                put("createdAt", dbCustomers.getLong(dbCustomers.getColumnIndexOrThrow("createdAt")))
            }
            custList.put(c)
        }
        dbCustomers.close()
        root.put("customers", custList)

        val dbTxns = database.openHelper.readableDatabase.query("SELECT * FROM transactions")
        val txnList = JSONArray()
        while (dbTxns.moveToNext()) {
            val t = JSONObject().apply {
                put("id", dbTxns.getLong(dbTxns.getColumnIndexOrThrow("id")))
                put("customerId", dbTxns.getLong(dbTxns.getColumnIndexOrThrow("customerId")))
                put("type", dbTxns.getString(dbTxns.getColumnIndexOrThrow("type")))
                put("amount", dbTxns.getDouble(dbTxns.getColumnIndexOrThrow("amount")))
                put("date", dbTxns.getLong(dbTxns.getColumnIndexOrThrow("date")))
                put("category", dbTxns.getString(dbTxns.getColumnIndexOrThrow("category")))
                put("invoiceNumber", dbTxns.getString(dbTxns.getColumnIndexOrThrow("invoiceNumber")))
                put("paymentMethod", dbTxns.getString(dbTxns.getColumnIndexOrThrow("paymentMethod")))
                put("note", dbTxns.getString(dbTxns.getColumnIndexOrThrow("note")))
            }
            txnList.put(t)
        }
        dbTxns.close()
        root.put("transactions", txnList)

        root.put("exportedAt", System.currentTimeMillis())
        root.put("version", 1)
        root.toString(2)
    }

    // Import data from JSON string
    suspend fun importDataFromJson(jsonStr: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonStr)
            val customersArray = root.optJSONArray("customers") ?: JSONArray()
            val txnsArray = root.optJSONArray("transactions") ?: JSONArray()

            val newCustomers = mutableListOf<Customer>()
            for (i in 0 until customersArray.length()) {
                val obj = customersArray.getJSONObject(i)
                newCustomers.add(
                    Customer(
                        id = obj.optLong("id", 0),
                        name = obj.getString("name"),
                        phone = obj.optString("phone", ""),
                        address = obj.optString("address", ""),
                        notes = obj.optString("notes", ""),
                        creditLimit = obj.optDouble("creditLimit", 0.0),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
            if (newCustomers.isNotEmpty()) {
                customerDao.insertAll(newCustomers)
            }

            val newTxns = mutableListOf<Transaction>()
            for (i in 0 until txnsArray.length()) {
                val obj = txnsArray.getJSONObject(i)
                newTxns.add(
                    Transaction(
                        id = obj.optLong("id", 0),
                        customerId = obj.getLong("customerId"),
                        type = obj.getString("type"),
                        amount = obj.getDouble("amount"),
                        date = obj.optLong("date", System.currentTimeMillis()),
                        category = obj.optString("category", "عام"),
                        invoiceNumber = obj.optString("invoiceNumber", ""),
                        paymentMethod = obj.optString("paymentMethod", "نقداً"),
                        note = obj.optString("note", "")
                    )
                )
            }
            if (newTxns.isNotEmpty()) {
                transactionDao.insertAll(newTxns)
            }

            Result.success(newCustomers.size + newTxns.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Populate demo data if database is empty
    suspend fun populateDemoDataIfEmpty() = withContext(Dispatchers.IO) {
        val check = database.openHelper.readableDatabase.query("SELECT COUNT(*) FROM customers")
        var count = 0
        if (check.moveToFirst()) {
            count = check.getInt(0)
        }
        check.close()
        if (count > 0) return@withContext

        seedRealisticDemoData()
    }

    suspend fun seedRealisticDemoData() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val day = 86_400_000L

        val c1 = customerDao.insertCustomer(
            Customer(name = "محمد عبد الله الأهدل", phone = "0551234567", address = "صنعاء - شارع حدة", notes = "عميل جملة ممتاز", creditLimit = 15000.0)
        )
        val c2 = customerDao.insertCustomer(
            Customer(name = "سالم أحمد باوزير", phone = "0509876543", address = "المكلا - الديس", notes = "سداد أسبوعي منتظم", creditLimit = 8000.0)
        )
        val c3 = customerDao.insertCustomer(
            Customer(name = "عمر خالد الشميري", phone = "0563456789", address = "تعز - التحرير الأسفل", notes = "مشتريات مواد بناء", creditLimit = 20000.0)
        )
        val c4 = customerDao.insertCustomer(
            Customer(name = "شركة الفجر للتجارة", phone = "0547788990", address = "عدن - المعلا", notes = "حساب تجاري معتمد", creditLimit = 50000.0)
        )
        val c5 = customerDao.insertCustomer(
            Customer(name = "عبد الرحمن العميسي", phone = "0531122334", address = "إب - الدائري الغربي", notes = "متجر مواد غذائية", creditLimit = 10000.0)
        )

        // c1 transactions
        transactionDao.insertTransaction(Transaction(customerId = c1, type = "DEBT", amount = 3500.0, category = "بضاعة مواد غذائية", invoiceNumber = "INV-101", date = now - 5 * day))
        transactionDao.insertTransaction(Transaction(customerId = c1, type = "DEBT", amount = 1200.0, category = "فاتورة إضافية", invoiceNumber = "INV-108", date = now - 3 * day))
        transactionDao.insertTransaction(Transaction(customerId = c1, type = "PAYMENT", amount = 2000.0, category = "سداد دفعة", paymentMethod = "نقداً", invoiceNumber = "REC-401", date = now - 1 * day))

        // c2 transactions
        transactionDao.insertTransaction(Transaction(customerId = c2, type = "DEBT", amount = 4800.0, category = "أجهزة وإلكترونيات", invoiceNumber = "INV-102", date = now - 7 * day))
        transactionDao.insertTransaction(Transaction(customerId = c2, type = "PAYMENT", amount = 4800.0, category = "سداد كامل", paymentMethod = "حوالة مصرفية", invoiceNumber = "REC-402", date = now - 2 * day))

        // c3 transactions
        transactionDao.insertTransaction(Transaction(customerId = c3, type = "DEBT", amount = 9200.0, category = "مشتريات أسمنت وحديد", invoiceNumber = "INV-103", date = now - 6 * day))
        transactionDao.insertTransaction(Transaction(customerId = c3, type = "PAYMENT", amount = 3000.0, category = "دفعة أولى", paymentMethod = "نقداً", invoiceNumber = "REC-403", date = now - 4 * day))

        // c4 transactions
        transactionDao.insertTransaction(Transaction(customerId = c4, type = "DEBT", amount = 18500.0, category = "طلبية بضاعة حاوية", invoiceNumber = "INV-104", date = now - 10 * day))
        transactionDao.insertTransaction(Transaction(customerId = c4, type = "PAYMENT", amount = 10000.0, category = "حوالة بنكية", paymentMethod = "تحويل مصرفي", invoiceNumber = "REC-404", date = now - 3 * day))

        // c5 transactions
        transactionDao.insertTransaction(Transaction(customerId = c5, type = "DEBT", amount = 2400.0, category = "مستلزمات متجر", invoiceNumber = "INV-105", date = now - 2 * day))

        // Incoming transfers
        transferDao.insertTransfer(
            Transfer(
                companyName = "الكريمي للصرافة",
                transferNumber = "KR-889210",
                amount = 2500.0,
                senderName = "أحمد عبد الله الأهدل",
                customerId = c1,
                date = now - 1 * day,
                status = "PENDING",
                note = "دفعة قسط عن طريق فرع حدة"
            )
        )
        transferDao.insertTransfer(
            Transfer(
                companyName = "النجم للحوالات",
                transferNumber = "NJ-554129",
                amount = 5000.0,
                senderName = "عمر خالد الشميري",
                customerId = c3,
                date = now - 2 * day,
                status = "PENDING",
                note = "سداد قيمة فواتير حديد"
            )
        )
        transferDao.insertTransfer(
            Transfer(
                companyName = "الراجحي تحويل",
                transferNumber = "RJ-100234",
                amount = 10000.0,
                senderName = "شركة الفجر",
                customerId = c4,
                date = now - 3 * day,
                status = "CONFIRMED",
                note = "تم التحقق وتأكيد القيد بحساب العميل"
            )
        )

        // Seed an SMS log
        smsDao.insertLog(
            SmsLog(
                customerId = c3,
                customerName = "عمر خالد الشميري",
                recipientPhone = "0563456789",
                messageText = "عزيزنا عمر خالد الشميري، رصيد حسابكم الحالي هو 6,200.00 ر.س. يرجى التكرم بالسداد في أقرب وقت. شاكرين حسن تعاونكم.",
                status = "SENT",
                sentAt = now - 1 * day
            )
        )
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        transactionDao.clearAll()
        customerDao.clearAll()
        transferDao.clearAll()
        smsDao.clearAll()
    }
}
