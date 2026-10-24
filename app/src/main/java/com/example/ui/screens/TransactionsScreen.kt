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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CustomerWithBalance
import com.example.data.model.Transaction
import com.example.ui.components.AppTopBar
import com.example.ui.components.TransactionCard
import com.example.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    transactions: List<Transaction>,
    customers: List<CustomerWithBalance>,
    currency: String,
    onBack: () -> Unit,
    onDeleteTransaction: (Transaction) -> Unit,
    onOpenSettings: () -> Unit
) {
    var filterType by remember { mutableStateOf("ALL") } // ALL, DEBT, PAYMENT
    var searchQuery by remember { mutableStateOf("") }
    val customerMap = remember(customers) { customers.associate { it.customer.id to it.customer.name } }

    val filteredList = remember(transactions, filterType, searchQuery, customerMap) {
        transactions.filter { txn ->
            val matchesType = when (filterType) {
                "DEBT" -> txn.type == "DEBT"
                "PAYMENT" -> txn.type == "PAYMENT"
                else -> true
            }
            val custName = customerMap[txn.customerId] ?: ""
            val matchesSearch = if (searchQuery.isBlank()) true else {
                val q = searchQuery.trim().lowercase(Locale.ROOT)
                custName.lowercase(Locale.ROOT).contains(q) ||
                txn.category.lowercase(Locale.ROOT).contains(q) ||
                txn.invoiceNumber.lowercase(Locale.ROOT).contains(q) ||
                txn.note.lowercase(Locale.ROOT).contains(q)
            }
            matchesType && matchesSearch
        }
    }

    val totalDebtFiltered = remember(filteredList) {
        filteredList.filter { it.type == "DEBT" }.sumOf { it.amount }
    }
    val totalPaidFiltered = remember(filteredList) {
        filteredList.filter { it.type == "PAYMENT" }.sumOf { it.amount }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "سجل المعاملات المالية",
                storeName = "إجمالي: ${filteredList.size} عملية",
                onOpenSettings = onOpenSettings,
                onNotificationsClick = {},
                canNavigateBack = true,
                onBack = onBack
            )
        },
        containerColor = BackgroundIce
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search and Filters Bar
            Surface(
                color = CardWhite,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("بحث باسم العميل، الفاتورة، أو الملاحظة...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "مسح")
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("transactions_search_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = filterType == "ALL",
                            onClick = { filterType = "ALL" },
                            label = { Text("كافة العمليات (${transactions.size})", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = filterType == "DEBT",
                            onClick = { filterType = "DEBT" },
                            label = { Text("ديون فقط", fontSize = 11.sp, color = RedDebt) }
                        )
                        FilterChip(
                            selected = filterType == "PAYMENT",
                            onClick = { filterType = "PAYMENT" },
                            label = { Text("سدادات فقط", fontSize = 11.sp, color = GreenPayment) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Summary bar
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = BackgroundIce,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "مجموع الديون: ${"%,.2f".format(Locale.US, totalDebtFiltered)} $currency",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = RedDebt
                            )
                            Text(
                                text = "مجموع المقبوض: ${"%,.2f".format(Locale.US, totalPaidFiltered)} $currency",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GreenPayment
                            )
                        }
                    }
                }
            }

            // List
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.ReceiptLong, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(56.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("لا توجد عمليات مالية تطابق المعايير", fontSize = 14.sp, color = TextSecondary)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 90.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList, key = { it.id }) { txn ->
                        TransactionCard(
                            transaction = txn,
                            customerName = customerMap[txn.customerId] ?: "عميل",
                            currency = currency,
                            onDelete = { onDeleteTransaction(txn) }
                        )
                    }
                }
            }
        }
    }
}
