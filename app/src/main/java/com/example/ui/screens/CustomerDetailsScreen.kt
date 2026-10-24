package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Customer
import com.example.data.model.CustomerWithBalance
import com.example.data.model.Transaction
import com.example.ui.components.AppTopBar
import com.example.ui.components.TransactionCard
import com.example.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailsScreen(
    customerWithBalance: CustomerWithBalance?,
    transactions: List<Transaction>,
    currency: String,
    onBack: () -> Unit,
    onRecordDebt: (Long) -> Unit,
    onRecordPayment: (Long) -> Unit,
    onCallClick: (String) -> Unit,
    onSmsClick: (CustomerWithBalance) -> Unit,
    onShareStatement: (CustomerWithBalance, List<Transaction>) -> Unit,
    onEditCustomer: (Customer) -> Unit,
    onDeleteCustomer: (Customer) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit
) {
    if (customerWithBalance == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("العميل غير موجود")
        }
        return
    }

    val customer = customerWithBalance.customer
    var filterType by remember { mutableStateOf("ALL") } // ALL, DEBT, PAYMENT
    var showDeleteDialog by remember { mutableStateOf(false) }

    val filteredTxns = remember(transactions, filterType) {
        when (filterType) {
            "DEBT" -> transactions.filter { it.type == "DEBT" }
            "PAYMENT" -> transactions.filter { it.type == "PAYMENT" }
            else -> transactions
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "كشف حساب: ${customer.name}",
                storeName = customer.phone,
                onOpenSettings = {},
                onNotificationsClick = {},
                canNavigateBack = true,
                onBack = onBack
            )
        },
        containerColor = BackgroundIce
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Customer Header Profile Card
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryNavy),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = customer.name.take(1),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CardWhite
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = customer.name,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryNavy
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (customer.phone.isNotBlank()) customer.phone else "بدون رقم هاتف",
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )
                                if (customer.address.isNotBlank()) {
                                    Text(
                                        text = customer.address,
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            // Edit / Delete buttons
                            Row {
                                IconButton(onClick = { onEditCustomer(customer) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = SecondaryBlue)
                                }
                                IconButton(onClick = { showDeleteDialog = true }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = RedDebt)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Divider(color = BackgroundIce, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(14.dp))

                        // Financial Balance Breakdown (الرصيد الحالي = مجموع الديون - مجموع المدفوعات)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text("مجموع الديون", fontSize = 11.sp, color = TextSecondary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "%,.2f".format(Locale.US, customerWithBalance.totalDebt),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RedDebt
                                )
                            }

                            Box(modifier = Modifier.width(1.dp).height(35.dp).background(BorderLight))

                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text("مجموع المدفوعات", fontSize = 11.sp, color = TextSecondary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "%,.2f".format(Locale.US, customerWithBalance.totalPaid),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GreenPayment
                                )
                            }

                            Box(modifier = Modifier.width(1.dp).height(35.dp).background(BorderLight))

                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text("الرصيد المتبقي", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "%,.2f".format(Locale.US, customerWithBalance.currentBalance),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (customerWithBalance.currentBalance > 0) RedDebt else GreenPayment
                                )
                            }
                        }
                    }
                }
            }

            // Quick Operations Buttons for this Customer
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onRecordDebt(customer.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = RedDebt),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("customer_detail_record_debt_btn")
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تسجيل دين", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onRecordPayment(customer.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPayment),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("customer_detail_record_payment_btn")
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تسجيل دفعة", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Communication and Share Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onShareStatement(customerWithBalance, transactions) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = SecondaryBlue)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("مشاركة كشف الحساب", fontSize = 12.sp, color = SecondaryBlue)
                    }

                    OutlinedButton(
                        onClick = { onSmsClick(customerWithBalance) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(16.dp), tint = PurpleAccent)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تذكير SMS", fontSize = 12.sp, color = PurpleAccent)
                    }

                    FilledTonalIconButton(
                        onClick = { onCallClick(customer.phone) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = "اتصال", tint = PrimaryNavy)
                    }
                }
            }

            // Transactions History Header & Filter
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "سجل العمليات المالية (${filteredTxns.size})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryNavy
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = filterType == "ALL",
                            onClick = { filterType = "ALL" },
                            label = { Text("الكل", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = filterType == "DEBT",
                            onClick = { filterType = "DEBT" },
                            label = { Text("ديون", fontSize = 11.sp, color = RedDebt) }
                        )
                        FilterChip(
                            selected = filterType == "PAYMENT",
                            onClick = { filterType = "PAYMENT" },
                            label = { Text("مدفوعات", fontSize = 11.sp, color = GreenPayment) }
                        )
                    }
                }
            }

            // Transactions List
            if (filteredTxns.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = CardWhite),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Outlined.ReceiptLong, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("لا توجد عمليات مالية مسجلة في هذا التبويب", fontSize = 13.sp, color = TextSecondary)
                        }
                    }
                }
            } else {
                items(filteredTxns, key = { it.id }) { txn ->
                    TransactionCard(
                        transaction = txn,
                        customerName = customer.name,
                        currency = currency,
                        onDelete = { onDeleteTransaction(txn) }
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("تأكيد حذف العميل", fontWeight = FontWeight.Bold, color = RedDebt) },
            text = { Text("هل أنت متأكد من حذف العميل (${customer.name})؟ سيتم حذف جميع الفواتير والمعاملات الخاصة به نهائياً.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteCustomer(customer)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedDebt)
                ) {
                    Text("نعم، احذف العميل", color = CardWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("إلغاء", color = TextSecondary)
                }
            }
        )
    }
}
