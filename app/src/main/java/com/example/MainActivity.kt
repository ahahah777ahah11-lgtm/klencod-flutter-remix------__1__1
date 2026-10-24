package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.Customer
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.BackgroundIce
import com.example.ui.theme.MyApplicationTheme
import com.example.util.configureAppCheck
import com.example.viewmodel.AppScreen
import com.example.viewmodel.DebtViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Mandated: Configure App Check before UI, ViewModels, or Firebase operations
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                SmartDebtManagerApp()
            }
        }
    }
}

@Composable
fun SmartDebtManagerApp(viewModel: DebtViewModel = viewModel()) {
    val context = LocalContext.current
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val customers by viewModel.allCustomersWithBalances.collectAsStateWithLifecycle()
    val filteredCustomers by viewModel.filteredCustomers.collectAsStateWithLifecycle()
    val transactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val recentTransactions by viewModel.recentTransactions.collectAsStateWithLifecycle()
    val totalDebts by viewModel.totalDebts.collectAsStateWithLifecycle()
    val totalPayments by viewModel.totalPayments.collectAsStateWithLifecycle()
    val customerCount by viewModel.customerCount.collectAsStateWithLifecycle()
    val transfers by viewModel.allTransfers.collectAsStateWithLifecycle()
    val smsLogs by viewModel.smsLogs.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCustomerIds by viewModel.selectedCustomerIds.collectAsStateWithLifecycle()
    val selectedCustomerId by viewModel.selectedCustomerId.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToast()
        }
    }

    // Dialog states
    var showRecordDebtDialog by remember { mutableStateOf(false) }
    var debtPreselectedCustomerId by remember { mutableStateOf<Long?>(null) }

    var showRecordPaymentDialog by remember { mutableStateOf(false) }
    var paymentPreselectedCustomerId by remember { mutableStateOf<Long?>(null) }

    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var customerToEdit by remember { mutableStateOf<Customer?>(null) }

    var showAddTransferDialog by remember { mutableStateOf(false) }
    var showQuickActionMenu by remember { mutableStateOf(false) }

    // Intercept back button when not on HOME screen
    BackHandler(enabled = currentScreen != AppScreen.HOME) {
        viewModel.navigateBack()
    }

    val pendingTransfersCount = remember(transfers) {
        transfers.count { it.status == "PENDING" }
    }

    val showBottomBar = currentScreen in listOf(
        AppScreen.HOME,
        AppScreen.CUSTOMERS,
        AppScreen.TRANSFERS,
        AppScreen.REPORTS
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                AppBottomNavigation(
                    currentScreen = currentScreen,
                    onNavigate = { screen -> viewModel.navigateTo(screen) },
                    onCenterFabClick = { showQuickActionMenu = true }
                )
            }
        },
        containerColor = BackgroundIce,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (showBottomBar) innerPadding.calculateBottomPadding() else innerPadding.calculateBottomPadding())
        ) {
            when (currentScreen) {
                AppScreen.HOME -> {
                    HomeScreen(
                        settings = settings,
                        totalDebts = totalDebts,
                        totalPayments = totalPayments,
                        customerCount = customerCount,
                        recentTransactions = recentTransactions,
                        customers = customers,
                        pendingTransfersCount = pendingTransfersCount,
                        onNavigate = { screen -> viewModel.navigateTo(screen) },
                        onOpenSettings = { viewModel.navigateTo(AppScreen.SETTINGS) },
                        onRecordDebtClick = {
                            debtPreselectedCustomerId = null
                            showRecordDebtDialog = true
                        },
                        onRecordPaymentClick = {
                            paymentPreselectedCustomerId = null
                            showRecordPaymentDialog = true
                        },
                        onAddCustomerClick = {
                            customerToEdit = null
                            showAddCustomerDialog = true
                        },
                        onAddTransferClick = {
                            showAddTransferDialog = true
                        },
                        onDeleteTransaction = { txn -> viewModel.deleteTransaction(txn) }
                    )
                }

                AppScreen.CUSTOMERS -> {
                    CustomersScreen(
                        customers = filteredCustomers,
                        searchQuery = searchQuery,
                        currency = settings.currency,
                        selectedCustomerIds = selectedCustomerIds,
                        onSearchChange = { viewModel.setSearchQuery(it) },
                        onCustomerClick = { custId ->
                            viewModel.navigateTo(AppScreen.CUSTOMER_DETAILS, custId)
                        },
                        onCallClick = { phone -> viewModel.dialCustomerPhone(context, phone) },
                        onSmsClick = { custWithBal ->
                            viewModel.clearCustomerSelection()
                            viewModel.toggleCustomerSelection(custWithBal.customer.id)
                            viewModel.navigateTo(AppScreen.SMS_REMINDERS)
                        },
                        onToggleSelect = { custId -> viewModel.toggleCustomerSelection(custId) },
                        onSelectAllDebtors = { viewModel.selectAllDebtors() },
                        onClearSelection = { viewModel.clearCustomerSelection() },
                        onAddCustomerClick = {
                            customerToEdit = null
                            showAddCustomerDialog = true
                        },
                        onBulkSmsClick = { viewModel.navigateTo(AppScreen.SMS_REMINDERS) },
                        onOpenSettings = { viewModel.navigateTo(AppScreen.SETTINGS) },
                        onNotificationsClick = { viewModel.navigateTo(AppScreen.TRANSFERS) }
                    )
                }

                AppScreen.CUSTOMER_DETAILS -> {
                    val activeCustomer = customers.find { it.customer.id == selectedCustomerId }
                    val customerTxns = transactions.filter { it.customerId == selectedCustomerId }

                    CustomerDetailsScreen(
                        customerWithBalance = activeCustomer,
                        transactions = customerTxns,
                        currency = settings.currency,
                        onBack = { viewModel.navigateBack() },
                        onRecordDebt = { custId ->
                            debtPreselectedCustomerId = custId
                            showRecordDebtDialog = true
                        },
                        onRecordPayment = { custId ->
                            paymentPreselectedCustomerId = custId
                            showRecordPaymentDialog = true
                        },
                        onCallClick = { phone -> viewModel.dialCustomerPhone(context, phone) },
                        onSmsClick = { custWithBal ->
                            viewModel.clearCustomerSelection()
                            viewModel.toggleCustomerSelection(custWithBal.customer.id)
                            viewModel.navigateTo(AppScreen.SMS_REMINDERS)
                        },
                        onShareStatement = { cust, txns ->
                            viewModel.shareAccountStatement(context, cust, txns)
                        },
                        onEditCustomer = { cust ->
                            customerToEdit = cust
                            showAddCustomerDialog = true
                        },
                        onDeleteCustomer = { cust -> viewModel.deleteCustomer(cust) },
                        onDeleteTransaction = { txn -> viewModel.deleteTransaction(txn) }
                    )
                }

                AppScreen.TRANSACTIONS -> {
                    TransactionsScreen(
                        transactions = transactions,
                        customers = customers,
                        currency = settings.currency,
                        onBack = { viewModel.navigateBack() },
                        onDeleteTransaction = { txn -> viewModel.deleteTransaction(txn) },
                        onOpenSettings = { viewModel.navigateTo(AppScreen.SETTINGS) }
                    )
                }

                AppScreen.TRANSFERS -> {
                    TransfersScreen(
                        transfers = transfers,
                        customers = customers,
                        currency = settings.currency,
                        onBack = { viewModel.navigateBack() },
                        onAddTransferClick = { showAddTransferDialog = true },
                        onConfirmTransfer = { transferId -> viewModel.confirmTransfer(transferId) },
                        onDeleteTransfer = { transfer -> viewModel.deleteTransfer(transfer) },
                        onOpenSettings = { viewModel.navigateTo(AppScreen.SETTINGS) }
                    )
                }

                AppScreen.SMS_REMINDERS -> {
                    SmsRemindersScreen(
                        customers = customers,
                        settings = settings,
                        smsLogs = smsLogs,
                        selectedCustomerIds = selectedCustomerIds,
                        onToggleSelect = { custId -> viewModel.toggleCustomerSelection(custId) },
                        onSelectAllDebtors = { viewModel.selectAllDebtors() },
                        onClearSelection = { viewModel.clearCustomerSelection() },
                        onSendDirectSms = { ctx, phone, msg, custId, custName ->
                            viewModel.sendSmsDirect(ctx, phone, msg, custId, custName)
                        },
                        onOpenSmsApp = { ctx, phone, msg ->
                            viewModel.openSmsApp(ctx, phone, msg)
                        },
                        onSaveDefaultTemplate = { newTpl ->
                            viewModel.updateSettings(settings.copy(defaultSmsTemplate = newTpl))
                        },
                        onBack = { viewModel.navigateBack() }
                    )
                }

                AppScreen.REPORTS -> {
                    ReportsScreen(
                        customers = customers,
                        transactions = transactions,
                        settings = settings,
                        totalDebts = totalDebts,
                        totalPayments = totalPayments,
                        onBack = { viewModel.navigateBack() },
                        onCustomerClick = { custId ->
                            viewModel.navigateTo(AppScreen.CUSTOMER_DETAILS, custId)
                        },
                        onShareStatement = { cust, txns ->
                            viewModel.shareAccountStatement(context, cust, txns)
                        },
                        onOpenSettings = { viewModel.navigateTo(AppScreen.SETTINGS) }
                    )
                }

                AppScreen.SETTINGS -> {
                    SettingsScreen(
                        currentSettings = settings,
                        onSaveSettings = { newSettings -> viewModel.updateSettings(newSettings) },
                        onNavigateToBackup = { viewModel.navigateTo(AppScreen.BACKUP_RESTORE) },
                        onBack = { viewModel.navigateBack() }
                    )
                }

                AppScreen.BACKUP_RESTORE -> {
                    val currentGoogleUser by viewModel.currentGoogleUser.collectAsStateWithLifecycle()
                    val cloudBackups by viewModel.cloudBackups.collectAsStateWithLifecycle()
                    val isCloudLoading by viewModel.isCloudLoading.collectAsStateWithLifecycle()

                    BackupRestoreScreen(
                        currentGoogleUser = currentGoogleUser,
                        cloudBackups = cloudBackups,
                        isCloudLoading = isCloudLoading,
                        onExportJson = { onReady -> viewModel.exportBackupJson(onReady) },
                        onImportJson = { jsonStr -> viewModel.importBackupJson(jsonStr) },
                        onSeedDemoData = { viewModel.seedDemoData() },
                        onClearDatabase = { viewModel.clearAllDatabase() },
                        onSignInGoogle = { viewModel.signInGoogle() },
                        onSignOutGoogle = { viewModel.signOutGoogle() },
                        onUploadCloudBackup = { viewModel.uploadCloudBackup() },
                        onRefreshCloudBackups = { viewModel.fetchCloudBackups() },
                        onRestoreCloudBackup = { backup -> viewModel.restoreCloudBackup(backup) },
                        onBack = { viewModel.navigateBack() }
                    )
                }

                AppScreen.AI_ASSISTANT -> {
                    val chatMessages by viewModel.aiAdvisorMessages.collectAsStateWithLifecycle()
                    val isAdvisorGenerating by viewModel.isAdvisorGenerating.collectAsStateWithLifecycle()
                    val analysisState by viewModel.analysisReportState.collectAsStateWithLifecycle()
                    val scannedReceiptState by viewModel.scannedReceiptState.collectAsStateWithLifecycle()

                    AiAssistantScreen(
                        settings = settings,
                        customers = customers,
                        totalDebts = totalDebts,
                        totalPayments = totalPayments,
                        chatMessages = chatMessages,
                        isAdvisorGenerating = isAdvisorGenerating,
                        analysisState = analysisState,
                        scannedReceiptState = scannedReceiptState,
                        onSendMessage = { query -> viewModel.sendAdvisorMessage(query) },
                        onRunAnalysis = { viewModel.runFinancialAnalysis() },
                        onScanReceipt = { bitmap -> viewModel.scanReceiptWithGemini(bitmap) },
                        onApplyScannedData = { data -> viewModel.applyScannedReceipt(data) },
                        onBack = { viewModel.navigateBack() }
                    )
                }
            }
        }
    }

    // Quick Action Center Menu Dialog
    if (showQuickActionMenu) {
        QuickActionMenuDialog(
            onDismiss = { showQuickActionMenu = false },
            onRecordDebt = {
                debtPreselectedCustomerId = null
                showRecordDebtDialog = true
            },
            onRecordPayment = {
                paymentPreselectedCustomerId = null
                showRecordPaymentDialog = true
            },
            onAddCustomer = {
                customerToEdit = null
                showAddCustomerDialog = true
            },
            onAddTransfer = {
                showAddTransferDialog = true
            },
            onSmsReminder = {
                viewModel.navigateTo(AppScreen.SMS_REMINDERS)
            },
            onAiAssistant = {
                viewModel.navigateTo(AppScreen.AI_ASSISTANT)
            }
        )
    }

    // Record Debt Dialog
    if (showRecordDebtDialog) {
        RecordDebtDialog(
            customers = customers,
            preSelectedCustomerId = debtPreselectedCustomerId,
            currency = settings.currency,
            onDismiss = { showRecordDebtDialog = false },
            onConfirm = { custId, amount, category, invoiceNo, note ->
                viewModel.recordDebt(custId, amount, category, invoiceNo, System.currentTimeMillis(), note)
            }
        )
    }

    // Record Payment Dialog
    if (showRecordPaymentDialog) {
        RecordPaymentDialog(
            customers = customers,
            preSelectedCustomerId = paymentPreselectedCustomerId,
            currency = settings.currency,
            onDismiss = { showRecordPaymentDialog = false },
            onConfirm = { custId, amount, paymentMethod, receiptNo, note ->
                viewModel.recordPayment(custId, amount, paymentMethod, receiptNo, System.currentTimeMillis(), note)
            }
        )
    }

    // Add or Edit Customer Dialog
    if (showAddCustomerDialog) {
        AddEditCustomerDialog(
            customer = customerToEdit,
            onDismiss = {
                showAddCustomerDialog = false
                customerToEdit = null
            },
            onConfirm = { name, phone, address, notes, limit, initialDebt ->
                if (customerToEdit == null) {
                    viewModel.addCustomer(name, phone, address, notes, limit, initialDebt)
                } else {
                    viewModel.updateCustomer(
                        customerToEdit!!.copy(
                            name = name,
                            phone = phone,
                            address = address,
                            notes = notes,
                            creditLimit = limit
                        )
                    )
                }
                customerToEdit = null
            }
        )
    }

    // Add Transfer Dialog
    if (showAddTransferDialog) {
        AddTransferDialog(
            customers = customers,
            currency = settings.currency,
            onDismiss = { showAddTransferDialog = false },
            onConfirm = { comp, transferNo, amt, sender, custId, note ->
                viewModel.addTransfer(comp, transferNo, amt, sender, custId, note)
            }
        )
    }
}
