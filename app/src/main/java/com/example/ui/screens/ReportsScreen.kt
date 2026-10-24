package com.example.ui.screens

import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSettings
import com.example.data.model.CustomerWithBalance
import com.example.data.model.Transaction
import com.example.ui.components.AppTopBar
import com.example.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    customers: List<CustomerWithBalance>,
    transactions: List<Transaction>,
    settings: AppSettings,
    totalDebts: Double,
    totalPayments: Double,
    onBack: () -> Unit,
    onCustomerClick: (Long) -> Unit,
    onShareStatement: (CustomerWithBalance, List<Transaction>) -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val netBalance = totalDebts - totalPayments
    val collectionRate = if (totalDebts > 0) ((totalPayments / totalDebts) * 100).coerceIn(0.0, 100.0) else 100.0

    val topDebtors = remember(customers) {
        customers.filter { it.currentBalance > 0 }.sortedByDescending { it.currentBalance }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "التقارير المالية وكشوف الحساب",
                storeName = settings.storeName,
                onOpenSettings = onOpenSettings,
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Collection Efficiency Card
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = PrimaryNavy),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("مؤشر التحصيل المالي العام", color = CardWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SecondaryBlue
                            ) {
                                Text(
                                    text = "${"%.1f".format(Locale.US, collectionRate)}%",
                                    color = CardWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Progress Bar
                        LinearProgressIndicator(
                            progress = { (collectionRate / 100f).toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = GreenPayment,
                            trackColor = TextSecondary.copy(alpha = 0.3f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("إجمالي الديون المطلوبة", color = TextSecondary, fontSize = 11.sp)
                                Text("${"%,.2f".format(Locale.US, totalDebts)} ${settings.currency}", color = RedDebtLight, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("إجمالي المبالغ المحصلة", color = TextSecondary, fontSize = 11.sp)
                                Text("${"%,.2f".format(Locale.US, totalPayments)} ${settings.currency}", color = GreenPaymentLight, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("المتبقي في السوق", color = TextSecondary, fontSize = 11.sp)
                                Text("${"%,.2f".format(Locale.US, netBalance)} ${settings.currency}", color = CardWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Top Debtors Ranking
            item {
                Text(
                    text = "ترتيب كبار المدينين (${topDebtors.size} عميل)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryNavy
                )
            }

            if (topDebtors.isEmpty()) {
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
                            Icon(Icons.Default.Verified, contentDescription = null, tint = GreenPayment, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("رائع! جميع العملاء خالصين ولا توجد أي ديون متأخرة", fontSize = 13.sp, color = PrimaryNavy, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                items(topDebtors.take(15)) { debtor ->
                    val rank = topDebtors.indexOf(debtor) + 1
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = CardWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCustomerClick(debtor.customer.id) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (rank <= 3) RedDebtLight else BackgroundIce),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$rank",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (rank <= 3) RedDebt else PrimaryNavy
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(debtor.customer.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryNavy)
                                Text(debtor.customer.phone.ifBlank { "بدون هاتف" }, fontSize = 11.sp, color = TextSecondary)
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${"%,.2f".format(Locale.US, debtor.currentBalance)} ${settings.currency}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = RedDebt
                                )
                                TextButton(
                                    onClick = { onShareStatement(debtor, transactions.filter { it.customerId == debtor.customer.id }) },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp), tint = SecondaryBlue)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("مشاركة كشف", fontSize = 10.sp, color = SecondaryBlue)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
