package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CustomerWithBalance
import com.example.ui.components.AppTopBar
import com.example.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(
    customers: List<CustomerWithBalance>,
    searchQuery: String,
    currency: String,
    selectedCustomerIds: Set<Long>,
    onSearchChange: (String) -> Unit,
    onCustomerClick: (Long) -> Unit,
    onCallClick: (String) -> Unit,
    onSmsClick: (CustomerWithBalance) -> Unit,
    onToggleSelect: (Long) -> Unit,
    onSelectAllDebtors: () -> Unit,
    onClearSelection: () -> Unit,
    onAddCustomerClick: () -> Unit,
    onBulkSmsClick: () -> Unit,
    onOpenSettings: () -> Unit,
    onNotificationsClick: () -> Unit
) {
    var filterMode by remember { mutableStateOf("ALL") } // ALL, DEBTORS, PAID
    val isSelectionMode = selectedCustomerIds.isNotEmpty()

    val filteredList = remember(customers, searchQuery, filterMode) {
        customers.filter { item ->
            val matchesFilter = when (filterMode) {
                "DEBTORS" -> item.currentBalance > 0
                "PAID" -> item.currentBalance <= 0
                else -> true
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                val q = searchQuery.trim().lowercase(Locale.ROOT)
                item.customer.name.lowercase(Locale.ROOT).contains(q) ||
                item.customer.phone.contains(q)
            }
            matchesFilter && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "قائمة العملاء والحسابات",
                storeName = "إجمالي: ${customers.size} عميل",
                onOpenSettings = onOpenSettings,
                onNotificationsClick = onNotificationsClick,
                canNavigateBack = false
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddCustomerClick,
                shape = CircleShape,
                containerColor = PrimaryNavy,
                contentColor = CardWhite,
                modifier = Modifier
                    .padding(bottom = 60.dp)
                    .testTag("add_customer_fab")
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "إضافة عميل جديد")
            }
        },
        containerColor = BackgroundIce
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Box & Multi-select Toolbar
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
                        onValueChange = onSearchChange,
                        placeholder = { Text("بحث بالاسم أو رقم الهاتف...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { onSearchChange("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "مسح")
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_search_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Filter chips row & Multi-select action
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = filterMode == "ALL",
                                onClick = { filterMode = "ALL" },
                                label = { Text("الكل (${customers.size})", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = filterMode == "DEBTORS",
                                onClick = { filterMode = "DEBTORS" },
                                label = {
                                    val count = customers.count { it.currentBalance > 0 }
                                    Text("عليهم ديون ($count)", fontSize = 11.sp, color = RedDebt)
                                }
                            )
                            FilterChip(
                                selected = filterMode == "PAID",
                                onClick = { filterMode = "PAID" },
                                label = {
                                    val count = customers.count { it.currentBalance <= 0 }
                                    Text("خالصين ($count)", fontSize = 11.sp, color = GreenPayment)
                                }
                            )
                        }

                        // Select all debtors button
                        TextButton(onClick = onSelectAllDebtors) {
                            Text("تحديد المدينين", fontSize = 11.sp, color = SecondaryBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Selection action banner
            AnimatedVisibility(visible = isSelectionMode) {
                Surface(
                    color = PrimaryNavy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "تم تحديد ${selectedCustomerIds.size} عميل",
                            color = CardWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row {
                            Button(
                                onClick = onBulkSmsClick,
                                colors = ButtonDefaults.buttonColors(containerColor = SecondaryBlue),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إرسال تذكير SMS", fontSize = 11.sp)
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            IconButton(onClick = onClearSelection) {
                                Icon(Icons.Default.Close, contentDescription = "إلغاء التحديد", tint = CardWhite)
                            }
                        }
                    }
                }
            }

            // Customers List
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.People,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "لم يتم العثور على أي عميل يطابق البحث" else "لا يوجد عملاء في هذه القائمة",
                            fontSize = 14.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList, key = { it.customer.id }) { item ->
                        CustomerCard(
                            item = item,
                            currency = currency,
                            isSelected = selectedCustomerIds.contains(item.customer.id),
                            onCardClick = { onCustomerClick(item.customer.id) },
                            onToggleSelect = { onToggleSelect(item.customer.id) },
                            onCallClick = { onCallClick(item.customer.phone) },
                            onSmsClick = { onSmsClick(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerCard(
    item: CustomerWithBalance,
    currency: String,
    isSelected: Boolean,
    onCardClick: () -> Unit,
    onToggleSelect: () -> Unit,
    onCallClick: () -> Unit,
    onSmsClick: () -> Unit
) {
    val hasDebt = item.currentBalance > 0
    val statusColor = if (hasDebt) RedDebt else GreenPayment
    val statusBg = if (hasDebt) RedDebtLight else GreenPaymentLight

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick)
            .testTag("customer_card_${item.customer.id}")
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
                // Checkbox for multi-select
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() }
                )

                // Avatar with initial
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (hasDebt) RedDebtLight else TurquoiseLight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.customer.name.take(1),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hasDebt) RedDebt else SecondaryBlue
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Name and Phone
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.customer.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryNavy,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (item.customer.phone.isNotBlank()) item.customer.phone else "بدون رقم هاتف",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                // Balance Badge
                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = statusBg
                    ) {
                        Text(
                            text = if (hasDebt) "مستحق عليه" else "خالص / له رصيد",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${"%,.2f".format(Locale.US, item.currentBalance)} $currency",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = BackgroundIce, thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons Row (تفاصيل الحساب، اتصال، إعداد رسالة SMS)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Details button
                TextButton(
                    onClick = onCardClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = SecondaryBlue, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تفاصيل الحساب (${item.transactionCount})", fontSize = 12.sp, color = SecondaryBlue, fontWeight = FontWeight.SemiBold)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Call Button
                    FilledTonalIconButton(
                        onClick = onCallClick,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = BackgroundIce,
                            contentColor = PrimaryNavy
                        ),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = "اتصال", modifier = Modifier.size(18.dp))
                    }

                    // SMS Button
                    FilledTonalIconButton(
                        onClick = onSmsClick,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = PurpleLight,
                            contentColor = PurpleAccent
                        ),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Sms, contentDescription = "إعداد رسالة SMS", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
