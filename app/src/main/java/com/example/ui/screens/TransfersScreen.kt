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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CustomerWithBalance
import com.example.data.model.Transfer
import com.example.ui.components.AppTopBar
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransfersScreen(
    transfers: List<Transfer>,
    customers: List<CustomerWithBalance>,
    currency: String,
    onBack: () -> Unit,
    onAddTransferClick: () -> Unit,
    onConfirmTransfer: (Long) -> Unit,
    onDeleteTransfer: (Transfer) -> Unit,
    onOpenSettings: () -> Unit
) {
    var statusFilter by remember { mutableStateOf("ALL") } // ALL, PENDING, CONFIRMED
    val customerMap = remember(customers) { customers.associate { it.customer.id to it.customer.name } }
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale.getDefault()) }

    val filteredTransfers = remember(transfers, statusFilter) {
        when (statusFilter) {
            "PENDING" -> transfers.filter { it.status == "PENDING" }
            "CONFIRMED" -> transfers.filter { it.status == "CONFIRMED" }
            else -> transfers
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "الحوالات المالية الواردة",
                storeName = "إجمالي: ${transfers.size} حوالة",
                onOpenSettings = onOpenSettings,
                onNotificationsClick = {},
                canNavigateBack = true,
                onBack = onBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTransferClick,
                shape = CircleShape,
                containerColor = AmberAccent,
                contentColor = CardWhite,
                modifier = Modifier
                    .padding(bottom = 60.dp)
                    .testTag("add_transfer_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "تسجيل حوالة جديدة")
            }
        },
        containerColor = BackgroundIce
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter Tabs & Info Banner
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
                    // System notice
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = AmberLight,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "يتم منع تكرار أرقام الحوالات نظامياً، ولا تُسجل كدفعة سداد في حساب العميل إلا عند الضغط على تأكيد الاستلام.",
                                fontSize = 11.sp,
                                color = PrimaryNavy,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val pendingCount = transfers.count { it.status == "PENDING" }
                        val confirmedCount = transfers.count { it.status == "CONFIRMED" }

                        FilterChip(
                            selected = statusFilter == "ALL",
                            onClick = { statusFilter = "ALL" },
                            label = { Text("الكل (${transfers.size})", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = statusFilter == "PENDING",
                            onClick = { statusFilter = "PENDING" },
                            label = { Text("قيد الانتظار ($pendingCount)", fontSize = 11.sp, color = AmberAccent) }
                        )
                        FilterChip(
                            selected = statusFilter == "CONFIRMED",
                            onClick = { statusFilter = "CONFIRMED" },
                            label = { Text("مستلمة ومؤكدة ($confirmedCount)", fontSize = 11.sp, color = GreenPayment) }
                        )
                    }
                }
            }

            // List
            if (filteredTransfers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.SwapHoriz, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(56.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("لا توجد حوالات في هذه القائمة", fontSize = 14.sp, color = TextSecondary)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredTransfers, key = { it.id }) { transfer ->
                        TransferCard(
                            transfer = transfer,
                            customerName = customerMap[transfer.customerId],
                            currency = currency,
                            dateFormat = dateFormat,
                            onConfirm = { onConfirmTransfer(transfer.id) },
                            onDelete = { onDeleteTransfer(transfer) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransferCard(
    transfer: Transfer,
    customerName: String?,
    currency: String,
    dateFormat: SimpleDateFormat,
    onConfirm: () -> Unit,
    onDelete: () -> Unit
) {
    val isPending = transfer.status == "PENDING"
    val statusColor = if (isPending) AmberAccent else GreenPayment
    val statusBg = if (isPending) AmberLight else GreenPaymentLight

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(statusBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPending) Icons.Default.HourglassEmpty else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transfer.companyName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryNavy
                    )
                    Text(
                        text = "رقم الحوالة: ${transfer.transferNumber}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SecondaryBlue
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = statusBg
                    ) {
                        Text(
                            text = if (isPending) "قيد الانتظار" else "تم القيد كدفعة",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${"%,.2f".format(Locale.US, transfer.amount)} $currency",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryNavy
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = BackgroundIce, thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Extra Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "المرسل: ${transfer.senderName.ifBlank { "غير مسجل" }}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "العميل المرتبط: ${customerName ?: "غير مرتبط بعميل"}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (customerName != null) PrimaryNavy else RedDebt
                    )
                }

                Text(
                    text = dateFormat.format(Date(transfer.date)),
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }

            if (transfer.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ملاحظات: ${transfer.note}",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            if (isPending) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = RedDebt)
                    }

                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPayment),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("confirm_transfer_btn_${transfer.id}")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تأكيد الاستلام وقيدها كدفعة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
