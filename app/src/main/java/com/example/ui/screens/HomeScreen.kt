package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSettings
import com.example.data.model.CustomerWithBalance
import com.example.data.model.Transaction
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.AppScreen
import java.util.Locale

@Composable
fun HomeScreen(
    settings: AppSettings,
    totalDebts: Double,
    totalPayments: Double,
    customerCount: Int,
    recentTransactions: List<Transaction>,
    customers: List<CustomerWithBalance>,
    pendingTransfersCount: Int,
    onNavigate: (AppScreen) -> Unit,
    onOpenSettings: () -> Unit,
    onRecordDebtClick: () -> Unit,
    onRecordPaymentClick: () -> Unit,
    onAddCustomerClick: () -> Unit,
    onAddTransferClick: () -> Unit,
    onDeleteTransaction: (Transaction) -> Unit
) {
    val netBalance = totalDebts - totalPayments
    val customerMap = customers.associate { it.customer.id to it.customer.name }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "مدير الديون الذكي SMS",
                storeName = settings.storeName,
                pendingTransfersCount = pendingTransfersCount,
                onOpenSettings = onOpenSettings,
                onNotificationsClick = { onNavigate(AppScreen.TRANSFERS) },
                canNavigateBack = false
            )
        },
        containerColor = BackgroundIce
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Financial Summary Cards (2x2 Grid)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "الملخص المالي العام",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryNavy
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SummaryStatCard(
                            title = "إجمالي الديون",
                            value = "%,.2f".format(Locale.US, totalDebts),
                            currency = settings.currency,
                            icon = Icons.Default.TrendingDown,
                            containerColor = RedDebtLight,
                            accentColor = RedDebt,
                            modifier = Modifier.weight(1f)
                        )

                        SummaryStatCard(
                            title = "إجمالي التحصيل",
                            value = "%,.2f".format(Locale.US, totalPayments),
                            currency = settings.currency,
                            icon = Icons.Default.TrendingUp,
                            containerColor = GreenPaymentLight,
                            accentColor = GreenPayment,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SummaryStatCard(
                            title = "صافي الرصيد المستحق",
                            value = "%,.2f".format(Locale.US, netBalance),
                            currency = settings.currency,
                            icon = Icons.Default.AccountBalance,
                            containerColor = BackgroundIce,
                            accentColor = PrimaryNavy,
                            modifier = Modifier.weight(1.3f)
                        )

                        SummaryStatCard(
                            title = "عدد العملاء",
                            value = customerCount.toString(),
                            currency = "عميل",
                            icon = Icons.Default.PeopleOutline,
                            containerColor = TurquoiseLight,
                            accentColor = SecondaryBlue,
                            modifier = Modifier.weight(0.9f)
                        )
                    }
                }
            }

            // Section 2: 3-Column Service Grid (شبكة الخدمات ذات الأعمدة الثلاثة)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "الخدمات والعمليات السريعة",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryNavy
                    )

                    // First Row of 3
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickServiceItem(
                            title = "تسجيل دين",
                            icon = Icons.Default.ArrowDownward,
                            color = RedDebt,
                            backgroundColor = RedDebtLight,
                            onClick = onRecordDebtClick,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_record_debt_btn")
                        )

                        QuickServiceItem(
                            title = "تسجيل دفعة",
                            icon = Icons.Default.ArrowUpward,
                            color = GreenPayment,
                            backgroundColor = GreenPaymentLight,
                            onClick = onRecordPaymentClick,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_record_payment_btn")
                        )

                        QuickServiceItem(
                            title = "إضافة عميل",
                            icon = Icons.Default.PersonAdd,
                            color = SecondaryBlue,
                            backgroundColor = BackgroundIce,
                            onClick = onAddCustomerClick,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_add_customer_btn")
                        )
                    }

                    // Second Row of 3
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickServiceItem(
                            title = "حوالات واردة",
                            icon = Icons.Default.SwapHoriz,
                            color = AmberAccent,
                            backgroundColor = AmberLight,
                            onClick = onAddTransferClick,
                            modifier = Modifier.weight(1f)
                        )

                        QuickServiceItem(
                            title = "رسائل SMS",
                            icon = Icons.Default.Sms,
                            color = PurpleAccent,
                            backgroundColor = PurpleLight,
                            onClick = { onNavigate(AppScreen.SMS_REMINDERS) },
                            modifier = Modifier.weight(1f)
                        )

                        QuickServiceItem(
                            title = "كشف الحساب",
                            icon = Icons.Default.Description,
                            color = TurquoiseAccent,
                            backgroundColor = TurquoiseLight,
                            onClick = { onNavigate(AppScreen.REPORTS) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Third Row: AI & Firebase Cloud
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickServiceItem(
                            title = "ذكاء Gemini",
                            icon = Icons.Default.AutoAwesome,
                            color = SecondaryBlue,
                            backgroundColor = TurquoiseLight,
                            onClick = { onNavigate(AppScreen.AI_ASSISTANT) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_gemini_ai_btn")
                        )

                        QuickServiceItem(
                            title = "ماسح الفواتير",
                            icon = Icons.Default.DocumentScanner,
                            color = PurpleAccent,
                            backgroundColor = PurpleLight,
                            onClick = { onNavigate(AppScreen.AI_ASSISTANT) },
                            modifier = Modifier.weight(1f)
                        )

                        QuickServiceItem(
                            title = "سحابة Firebase",
                            icon = Icons.Default.CloudSync,
                            color = GreenPayment,
                            backgroundColor = GreenPaymentLight,
                            onClick = { onNavigate(AppScreen.BACKUP_RESTORE) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Section 3: Recent Transactions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "أحدث المعاملات المالية",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryNavy
                    )

                    TextButton(
                        onClick = { onNavigate(AppScreen.TRANSACTIONS) },
                        modifier = Modifier.testTag("view_all_transactions_btn")
                    ) {
                        Text(
                            text = "عرض الكل",
                            fontSize = 13.sp,
                            color = SecondaryBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (recentTransactions.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = CardWhite),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ReceiptLong,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "لا توجد معاملات مسجلة حتى الآن",
                                fontSize = 14.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            } else {
                items(recentTransactions, key = { it.id }) { txn ->
                    TransactionCard(
                        transaction = txn,
                        customerName = customerMap[txn.customerId] ?: "عميل",
                        currency = settings.currency,
                        onDelete = { onDeleteTransaction(txn) }
                    )
                }
            }
        }
    }
}
