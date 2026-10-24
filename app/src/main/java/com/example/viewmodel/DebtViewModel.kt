package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telephony.SmsManager
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.AppSettings
import com.example.data.model.Customer
import com.example.data.model.CustomerWithBalance
import com.example.data.model.SmsLog
import com.example.data.model.Transaction
import com.example.data.model.Transfer
import com.example.data.repository.DebtRepository
import android.graphics.Bitmap
import com.example.ai.AiOperationState
import com.example.ai.GeminiDebtAiService
import com.example.ai.ScannedReceiptData
import com.example.data.firebase.CloudBackupMetadata
import com.example.data.firebase.FirestoreBackupManager
import com.example.ui.screens.ChatMessage
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AppScreen {
    HOME,
    CUSTOMERS,
    CUSTOMER_DETAILS,
    TRANSACTIONS,
    TRANSFERS,
    SMS_REMINDERS,
    REPORTS,
    SETTINGS,
    BACKUP_RESTORE,
    AI_ASSISTANT
}

class DebtViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = DebtRepository(database, application)

    // Current Navigation Screen
    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Screen Backstack for BackHandler
    private val screenStack = mutableListOf(AppScreen.HOME)

    // Selected customer for details screen
    private val _selectedCustomerId = MutableStateFlow<Long?>(null)
    val selectedCustomerId: StateFlow<Long?> = _selectedCustomerId.asStateFlow()

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Multi-select customers (for bulk SMS)
    private val _selectedCustomerIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedCustomerIds: StateFlow<Set<Long>> = _selectedCustomerIds.asStateFlow()

    // App Settings
    private val _settings = MutableStateFlow(repository.getSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    // Feedback message (snackbar/toast)
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // Repository flows
    val allCustomersWithBalances: StateFlow<List<CustomerWithBalance>> = repository.customersWithBalances
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered customers list based on search query
    val filteredCustomers: StateFlow<List<CustomerWithBalance>> = combine(
        allCustomersWithBalances,
        _searchQuery
    ) { list, query ->
        if (query.isBlank()) {
            list
        } else {
            val q = query.trim().lowercase(Locale.ROOT)
            list.filter {
                it.customer.name.lowercase(Locale.ROOT).contains(q) ||
                it.customer.phone.contains(q)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentTransactions: StateFlow<List<Transaction>> = repository.recentTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalDebts: StateFlow<Double> = repository.totalDebts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalPayments: StateFlow<Double> = repository.totalPayments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val customerCount: StateFlow<Int> = repository.customerCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val allTransfers: StateFlow<List<Transfer>> = repository.allTransfers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val smsLogs: StateFlow<List<SmsLog>> = repository.smsLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Gemini AI Service & Firebase Cloud Backup Manager
    val geminiAiService = GeminiDebtAiService()
    val firestoreBackupManager = FirestoreBackupManager(application)

    // Chat with AI Advisor (Gemini 3.8 Flash)
    private val _aiAdvisorMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                isUser = false,
                text = "مرحباً بك! أنا مستشارك المالي الذكي المدعوم بـ Google Gemini 3.8 Flash. كيف يمكنني مساعدتك اليوم في إدارة ديون متجرك، تسريع التحصيل، أو حل مشكلات العملاء؟"
            )
        )
    )
    val aiAdvisorMessages: StateFlow<List<ChatMessage>> = _aiAdvisorMessages.asStateFlow()

    private val _isAdvisorGenerating = MutableStateFlow(false)
    val isAdvisorGenerating: StateFlow<Boolean> = _isAdvisorGenerating.asStateFlow()

    // Financial Analysis Report
    private val _analysisReportState = MutableStateFlow<AiOperationState<String>>(AiOperationState.Idle)
    val analysisReportState: StateFlow<AiOperationState<String>> = _analysisReportState.asStateFlow()

    // Scanned Receipt Data
    private val _scannedReceiptState = MutableStateFlow<AiOperationState<ScannedReceiptData>>(AiOperationState.Idle)
    val scannedReceiptState: StateFlow<AiOperationState<ScannedReceiptData>> = _scannedReceiptState.asStateFlow()

    // Cloud Backups from Firestore
    private val _cloudBackups = MutableStateFlow<List<CloudBackupMetadata>>(emptyList())
    val cloudBackups: StateFlow<List<CloudBackupMetadata>> = _cloudBackups.asStateFlow()

    private val _isCloudLoading = MutableStateFlow(false)
    val isCloudLoading: StateFlow<Boolean> = _isCloudLoading.asStateFlow()

    private val _currentGoogleUser = MutableStateFlow(firestoreBackupManager.currentUser)
    val currentGoogleUser: StateFlow<FirebaseUser?> = _currentGoogleUser.asStateFlow()

    init {
        viewModelScope.launch {
            repository.populateDemoDataIfEmpty()
            if (firestoreBackupManager.currentUser != null) {
                fetchCloudBackups()
            }
        }
    }

    // Navigation
    fun navigateTo(screen: AppScreen, customerId: Long? = null) {
        if (customerId != null) {
            _selectedCustomerId.value = customerId
        }
        if (_currentScreen.value != screen) {
            screenStack.add(screen)
            _currentScreen.value = screen
        }
    }

    fun navigateBack(): Boolean {
        if (screenStack.size > 1) {
            screenStack.removeAt(screenStack.size - 1)
            _currentScreen.value = screenStack.last()
            return true
        } else if (_currentScreen.value != AppScreen.HOME) {
            _currentScreen.value = AppScreen.HOME
            screenStack.clear()
            screenStack.add(AppScreen.HOME)
            return true
        }
        return false
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
        Toast.makeText(getApplication(), msg, Toast.LENGTH_SHORT).show()
    }

    // Customer operations
    fun addCustomer(name: String, phone: String, address: String, notes: String, creditLimit: Double, initialDebt: Double) {
        if (name.isBlank()) {
            showToast("يرجى إدخال اسم العميل")
            return
        }
        viewModelScope.launch {
            val customer = Customer(
                name = name.trim(),
                phone = phone.trim(),
                address = address.trim(),
                notes = notes.trim(),
                creditLimit = creditLimit
            )
            val newId = repository.insertCustomer(customer)
            if (initialDebt > 0) {
                repository.recordDebt(
                    customerId = newId,
                    amount = initialDebt,
                    category = "رصيد افتتاحي",
                    invoiceNumber = "INIT",
                    date = System.currentTimeMillis(),
                    note = "دين سابق مسجل كرصيد افتتاحي"
                )
            }
            showToast("تمت إضافة العميل بنجاح")
        }
    }

    fun updateCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.updateCustomer(customer)
            showToast("تم تحديث بيانات العميل")
        }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
            if (_selectedCustomerId.value == customer.id) {
                navigateBack()
            }
            showToast("تم حذف العميل وسجل عملياته")
        }
    }

    // Debt and Payment Recording
    fun recordDebt(customerId: Long, amount: Double, category: String, invoiceNo: String, date: Long, note: String) {
        if (amount <= 0) {
            showToast("يرجى إدخال مبلغ صحيح أكبر من صفر")
            return
        }
        viewModelScope.launch {
            repository.recordDebt(customerId, amount, category, invoiceNo, date, note)
            showToast("تم تسجيل الدين بنجاح ($amount ${_settings.value.currency})")
        }
    }

    fun recordPayment(customerId: Long, amount: Double, paymentMethod: String, receiptNo: String, date: Long, note: String) {
        if (amount <= 0) {
            showToast("يرجى إدخال مبلغ صحيح أكبر من صفر")
            return
        }
        viewModelScope.launch {
            repository.recordPayment(customerId, amount, paymentMethod, receiptNo, date, note)
            showToast("تم تسجيل الدفعة بنجاح ($amount ${_settings.value.currency})")
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            showToast("تم حذف العملية المالية وتحديث الرصيد تلقائياً")
        }
    }

    // Transfers
    fun addTransfer(company: String, transferNo: String, amount: Double, sender: String, customerId: Long?, note: String) {
        if (transferNo.isBlank()) {
            showToast("يرجى إدخال رقم الحوالة")
            return
        }
        if (amount <= 0) {
            showToast("يرجى إدخال مبلغ الحوالة بشكل صحيح")
            return
        }
        viewModelScope.launch {
            val transfer = Transfer(
                companyName = company.ifBlank { "شركة صرافة" },
                transferNumber = transferNo.trim(),
                amount = amount,
                senderName = sender.ifBlank { "مرسل غير محدد" },
                customerId = customerId,
                note = note,
                status = "PENDING"
            )
            val result = repository.recordTransfer(transfer)
            result.onSuccess {
                showToast("تم تسجيل الحوالة برقم $transferNo (قيد الاستلام)")
            }.onFailure {
                showToast(it.message ?: "فشل تسجيل الحوالة")
            }
        }
    }

    fun confirmTransfer(transferId: Long) {
        viewModelScope.launch {
            val result = repository.confirmTransfer(transferId)
            result.onSuccess {
                showToast("تم تأكيد استلام الحوالة وقيدها كدفعة مسددة في حساب العميل بنجاح!")
            }.onFailure {
                showToast(it.message ?: "فشل تأكيد الحوالة")
            }
        }
    }

    fun deleteTransfer(transfer: Transfer) {
        viewModelScope.launch {
            repository.deleteTransfer(transfer)
            showToast("تم حذف الحوالة")
        }
    }

    // SMS & Reminders
    fun toggleCustomerSelection(customerId: Long) {
        val current = _selectedCustomerIds.value.toMutableSet()
        if (current.contains(customerId)) {
            current.remove(customerId)
        } else {
            current.add(customerId)
        }
        _selectedCustomerIds.value = current
    }

    fun selectAllDebtors() {
        val debtors = allCustomersWithBalances.value.filter { it.currentBalance > 0 }.map { it.customer.id }
        _selectedCustomerIds.value = debtors.toSet()
    }

    fun clearCustomerSelection() {
        _selectedCustomerIds.value = emptySet()
    }

    fun renderSms(template: String, customer: Customer, balance: Double): String {
        return repository.renderSmsMessage(template, customer.name, balance, _settings.value.currency)
    }

    fun sendSmsDirect(context: Context, phone: String, message: String, customerId: Long? = null, customerName: String = "") {
        if (phone.isBlank()) {
            showToast("رقم الهاتف غير متوفر للعميل")
            return
        }
        try {
            val smsManager = context.getSystemService(SmsManager::class.java)
                ?: SmsManager.getDefault()
            val parts = smsManager.divideMessage(message)
            smsManager.sendMultipartTextMessage(phone, null, parts, null, null)

            viewModelScope.launch {
                repository.logSms(
                    SmsLog(
                        customerId = customerId,
                        customerName = customerName,
                        recipientPhone = phone,
                        messageText = message,
                        status = "SENT"
                    )
                )
            }
            showToast("تم إرسال رسالة التذكير بنجاح إلى $phone")
        } catch (e: Exception) {
            // Fallback to SMS App Intent
            openSmsApp(context, phone, message)
        }
    }

    fun openSmsApp(context: Context, phone: String, message: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${phone.trim()}")
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            showToast("تعذر فتح تطبيق الرسائل: ${e.message}")
        }
    }

    fun dialCustomerPhone(context: Context, phone: String) {
        if (phone.isBlank()) {
            showToast("رقم الهاتف غير مسجل")
            return
        }
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${phone.trim()}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            showToast("تعذر فتح لوحة الاتصال")
        }
    }

    fun shareAccountStatement(context: Context, customer: CustomerWithBalance, transactions: List<Transaction>) {
        val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        val sb = StringBuilder()
        sb.append("══════════════════════════════\n")
        sb.append("📋 كشف حساب عميل\n")
        sb.append("المتجر: ${_settings.value.storeName}\n")
        sb.append("العميل: ${customer.customer.name}\n")
        sb.append("الهاتف: ${customer.customer.phone}\n")
        sb.append("التاريخ: ${dateFormat.format(Date())}\n")
        sb.append("══════════════════════════════\n\n")

        sb.append("📊 الملخص المالي:\n")
        sb.append("• إجمالي الديون: ${"%,.2f".format(Locale.US, customer.totalDebt)} ${_settings.value.currency}\n")
        sb.append("• إجمالي المسدد: ${"%,.2f".format(Locale.US, customer.totalPaid)} ${_settings.value.currency}\n")
        sb.append("• الرصيد المتبقي المستحق: ${"%,.2f".format(Locale.US, customer.currentBalance)} ${_settings.value.currency}\n\n")

        sb.append("📜 تفاصيل العمليات الأخيرة:\n")
        transactions.take(15).forEach { txn ->
            val dateStr = dateFormat.format(Date(txn.date))
            val typeStr = if (txn.type == "DEBT") "🔴 دين" else "🟢 دفعة"
            sb.append("[$dateStr] $typeStr : ${"%,.2f".format(Locale.US, txn.amount)} ${_settings.value.currency} - ${txn.category}")
            if (txn.note.isNotBlank()) sb.append(" (${txn.note})")
            sb.append("\n")
        }
        sb.append("\n══════════════════════════════\n")
        sb.append("شكراً لتعاملكم معنا 🙏")

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, sb.toString())
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "مشاركة كشف الحساب")
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }

    // Settings
    fun updateSettings(newSettings: AppSettings) {
        repository.saveSettings(newSettings)
        _settings.value = newSettings
        showToast("تم حفظ الإعدادات بنجاح")
    }

    // Backup & Restore
    fun exportBackupJson(onReady: (String) -> Unit) {
        viewModelScope.launch {
            val json = repository.exportDataAsJson()
            onReady(json)
            showToast("تم تصدير النسخة الاحتياطية بنجاح")
        }
    }

    fun importBackupJson(jsonStr: String) {
        viewModelScope.launch {
            val result = repository.importDataFromJson(jsonStr)
            result.onSuccess { count ->
                showToast("تمت استعادة $count سجل بنجاح!")
            }.onFailure {
                showToast("فشل استيراد النسخة الاحتياطية: ${it.message}")
            }
        }
    }

    fun seedDemoData() {
        viewModelScope.launch {
            repository.seedRealisticDemoData()
            showToast("تم توليد بيانات مالية وتجريبية بنجاح!")
        }
    }

    fun clearAllDatabase() {
        viewModelScope.launch {
            repository.clearAllData()
            showToast("تم مسح كافة البيانات وإعادة تعيين قاعدة البيانات")
        }
    }

    // ==========================================
    // Gemini 3.8 Flash AI Features
    // ==========================================

    fun sendAdvisorMessage(query: String) {
        val userMsg = ChatMessage(isUser = true, text = query)
        _aiAdvisorMessages.value = _aiAdvisorMessages.value + userMsg
        _isAdvisorGenerating.value = true

        val financialContext = "المتجر: ${_settings.value.storeName}, إجمالي الديون: ${totalDebts.value} ${_settings.value.currency}, إجمالي التحصيل: ${totalPayments.value} ${_settings.value.currency}, عدد العملاء: ${customerCount.value}"

        viewModelScope.launch {
            try {
                val accumulated = StringBuilder()
                var modelMsgAdded = false
                geminiAiService.chatWithAdvisorStream(query, financialContext).collect { token ->
                    accumulated.append(token)
                    if (!modelMsgAdded) {
                        _aiAdvisorMessages.value = _aiAdvisorMessages.value + ChatMessage(isUser = false, text = accumulated.toString())
                        modelMsgAdded = true
                    } else {
                        val currentList = _aiAdvisorMessages.value.toMutableList()
                        currentList[currentList.size - 1] = ChatMessage(isUser = false, text = accumulated.toString())
                        _aiAdvisorMessages.value = currentList
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiAdvisorMessages.value = _aiAdvisorMessages.value + ChatMessage(
                    isUser = false,
                    text = "عذراً، حدث خطأ أثناء الاتصال بنموذج Gemini: ${e.localizedMessage ?: "تأكد من الاتصال بالإنترنت"}"
                )
            } finally {
                _isAdvisorGenerating.value = false
            }
        }
    }

    fun runFinancialAnalysis() {
        _analysisReportState.value = AiOperationState.Loading
        viewModelScope.launch {
            try {
                val debtorsSummary = allCustomersWithBalances.value
                    .filter { it.currentBalance > 0 }
                    .take(15)
                    .joinToString("\n") { "- ${it.customer.name}: رصيد متبقي ${it.currentBalance} ${_settings.value.currency} (ديون: ${it.totalDebt}, مسدد: ${it.totalPaid})" }

                val report = geminiAiService.analyzeFinancialPortfolio(
                    totalDebts = totalDebts.value,
                    totalPayments = totalPayments.value,
                    customerCount = customerCount.value,
                    debtorsSummary = debtorsSummary.ifBlank { "لا يوجد عملاء مدينون حالياً." },
                    currency = _settings.value.currency
                )
                _analysisReportState.value = AiOperationState.Success(report)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _analysisReportState.value = AiOperationState.Error(e.localizedMessage ?: "فشل التحليل المالي")
            }
        }
    }

    fun scanReceiptWithGemini(bitmap: Bitmap) {
        _scannedReceiptState.value = AiOperationState.Loading
        viewModelScope.launch {
            try {
                val result = geminiAiService.scanReceiptOrInvoice(bitmap)
                _scannedReceiptState.value = AiOperationState.Success(result)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _scannedReceiptState.value = AiOperationState.Error(e.localizedMessage ?: "فشل فحص المستند")
            }
        }
    }

    fun applyScannedReceipt(data: ScannedReceiptData) {
        viewModelScope.launch {
            val existingCustomer = allCustomersWithBalances.value.find {
                it.customer.name.trim().equals(data.customerName.trim(), ignoreCase = true)
            }
            val customerId = if (existingCustomer != null) {
                existingCustomer.customer.id
            } else {
                val newCust = Customer(
                    name = data.customerName.ifBlank { "عميل جديد" },
                    phone = "",
                    address = "",
                    notes = "أضيف تلقائياً عبر ماسح الفواتير الذكي Gemini"
                )
                repository.insertCustomer(newCust)
            }

            if (data.transactionType == "DEBT") {
                recordDebt(
                    customerId = customerId,
                    amount = data.amount,
                    category = data.category.ifBlank { "فاتورة مبيعات" },
                    invoiceNo = data.invoiceOrReceiptNo.ifBlank { "INV-SCAN" },
                    date = System.currentTimeMillis(),
                    note = data.notes.ifBlank { "مسجلة آلياً بواسطة ماسح Gemini" }
                )
            } else {
                recordPayment(
                    customerId = customerId,
                    amount = data.amount,
                    paymentMethod = data.category.ifBlank { "سند قبض" },
                    receiptNo = data.invoiceOrReceiptNo.ifBlank { "REC-SCAN" },
                    date = System.currentTimeMillis(),
                    note = data.notes.ifBlank { "مسجلة آلياً بواسطة ماسح Gemini" }
                )
            }
            _scannedReceiptState.value = AiOperationState.Idle
            showToast("تم تسجيل العملية بنجاح في حساب العميل ${data.customerName}!")
        }
    }

    fun draftAiReminder(customer: CustomerWithBalance, tone: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val msg = geminiAiService.generateCollectionMessage(
                    customerName = customer.customer.name,
                    amount = customer.currentBalance,
                    currency = _settings.value.currency,
                    storeName = _settings.value.storeName,
                    tone = tone
                )
                onResult(msg)
                showToast("تمت صياغة الرسالة بنجاح بواسطة Gemini")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showToast("تعذر صياغة الرسالة: ${e.localizedMessage}")
            }
        }
    }

    // ==========================================
    // Firebase Firestore Cloud Backup & Sync
    // ==========================================

    fun signInGoogle() {
        _isCloudLoading.value = true
        viewModelScope.launch {
            val result = firestoreBackupManager.signInWithGoogle()
            result.onSuccess { user ->
                _currentGoogleUser.value = user
                showToast("تم تسجيل الدخول بنجاح: ${user.email ?: user.displayName}")
                fetchCloudBackups()
            }.onFailure { err ->
                showToast(err.message ?: "فشل تسجيل الدخول باستخدام Google")
            }
            _isCloudLoading.value = false
        }
    }

    fun signOutGoogle() {
        firestoreBackupManager.signOut()
        _currentGoogleUser.value = null
        _cloudBackups.value = emptyList()
        showToast("تم تسجيل الخروج من حساب Google")
    }

    fun uploadCloudBackup() {
        val user = _currentGoogleUser.value
        if (user == null) {
            signInGoogle()
            return
        }

        _isCloudLoading.value = true
        viewModelScope.launch {
            val jsonStr = repository.exportDataAsJson()
            val result = firestoreBackupManager.uploadBackupToFirestore(
                backupJson = jsonStr,
                storeName = _settings.value.storeName,
                customerCount = customerCount.value,
                transactionCount = allTransactions.value.size
            )
            result.onSuccess {
                showToast("تم رفع النسخة الاحتياطية بنجاح إلى سحابة Firebase Firestore!")
                fetchCloudBackups()
            }.onFailure { err ->
                showToast("فشل الرفع السحابي: ${err.message}")
            }
            _isCloudLoading.value = false
        }
    }

    fun fetchCloudBackups() {
        if (_currentGoogleUser.value == null) return
        _isCloudLoading.value = true
        viewModelScope.launch {
            val result = firestoreBackupManager.fetchCloudBackups()
            result.onSuccess { list ->
                _cloudBackups.value = list
            }.onFailure { err ->
                showToast("تعذر جلب النسخ السحابية: ${err.message}")
            }
            _isCloudLoading.value = false
        }
    }

    fun restoreCloudBackup(backup: CloudBackupMetadata) {
        viewModelScope.launch {
            val result = repository.importDataFromJson(backup.dataJson)
            result.onSuccess { count ->
                showToast("تمت استعادة $count سجل بنجاح من سحابة Firebase!")
            }.onFailure { err ->
                showToast("فشلت الاستعادة: ${err.message}")
            }
        }
    }
}
