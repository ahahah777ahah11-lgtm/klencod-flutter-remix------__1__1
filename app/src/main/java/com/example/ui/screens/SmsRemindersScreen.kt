package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.ai.GeminiDebtAiService
import com.example.data.model.AppSettings
import com.example.data.model.CustomerWithBalance
import com.example.data.model.SmsLog
import com.example.ui.components.AppTopBar
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsRemindersScreen(
    customers: List<CustomerWithBalance>,
    settings: AppSettings,
    smsLogs: List<SmsLog>,
    selectedCustomerIds: Set<Long>,
    onToggleSelect: (Long) -> Unit,
    onSelectAllDebtors: () -> Unit,
    onClearSelection: () -> Unit,
    onSendDirectSms: (context: Context, phone: String, message: String, customerId: Long?, customerName: String) -> Unit,
    onOpenSmsApp: (context: Context, phone: String, message: String) -> Unit,
    onSaveDefaultTemplate: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0: Compose & Send, 1: Sent SMS Logs
    var templateText by remember { mutableStateOf(settings.defaultSmsTemplate) }
    var showAiDrafterDialog by remember { mutableStateOf(false) }
    var selectedTone by remember { mutableStateOf("رسمي ومهني") }
    var isGeneratingAiMessage by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val aiService = remember { GeminiDebtAiService() }

    // Sample customer for preview
    val debtors = remember(customers) { customers.filter { it.currentBalance > 0 } }
    val previewTarget = remember(debtors, customers) {
        debtors.firstOrNull() ?: customers.firstOrNull()
    }

    // Live preview generator
    val livePreviewMessage = remember(templateText, previewTarget, settings.currency, settings.storeName) {
        val name = previewTarget?.customer?.name ?: "سالم أحمد"
        val balance = "%,.2f".format(Locale.US, previewTarget?.currentBalance ?: 4500.0)
        val date = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date())

        templateText
            .replace("{name}", name)
            .replace("{اسم_العميل}", name)
            .replace("{balance}", balance)
            .replace("{المبلغ_المستحق}", balance)
            .replace("{currency}", settings.currency)
            .replace("{العملة}", settings.currency)
            .replace("{date}", date)
            .replace("{تاريخ_اليوم}", date)
            .replace("{store}", settings.storeName)
            .replace("{اسم_المحل}", settings.storeName)
    }

    // Permission launcher for SEND_SMS
    var pendingSendAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingSendAction?.invoke()
        } else {
            // Permission denied -> fallback to opening SMS app
            pendingSendAction?.invoke()
        }
        pendingSendAction = null
    }

    fun requestAndSend(action: () -> Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            action()
        } else {
            pendingSendAction = action
            smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "مدير رسائل SMS والتذكير بالديون",
                storeName = "قوالب وإرسال تذكيرات نصية",
                onOpenSettings = {},
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
            // Tab Switcher
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = CardWhite,
                contentColor = PrimaryNavy
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("إعداد وإرسال التذكيرات", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("سجل الرسائل المرسلة (${smsLogs.size})", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            if (selectedTab == 0) {
                // Compose Tab
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Template Card
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
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
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "قالب رسالة التذكير:",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryNavy
                                    )

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = TurquoiseLight,
                                            modifier = Modifier.clickable { showAiDrafterDialog = true }
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = SecondaryBlue, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("صياغة ذكية (Gemini)", fontSize = 11.sp, color = SecondaryBlue, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))

                                        TextButton(onClick = { onSaveDefaultTemplate(templateText) }) {
                                            Text("حفظ", fontSize = 11.sp, color = SecondaryBlue, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                OutlinedTextField(
                                    value = templateText,
                                    onValueChange = { templateText = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp)
                                        .testTag("sms_template_input"),
                                    maxLines = 5
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Text("المتغيرات التلقائية (انقر للإدراج):", fontSize = 11.sp, color = TextSecondary)
                                Spacer(modifier = Modifier.height(4.dp))

                                // Variable insertion chips
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val tags = listOf("{اسم_العميل}", "{المبلغ_المستحق}", "{العملة}", "{تاريخ_اليوم}")
                                    tags.forEach { tag ->
                                        SuggestionChip(
                                            onClick = { templateText = "$templateText $tag" },
                                            label = { Text(tag, fontSize = 10.sp) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Live Message Preview Box
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = PurpleLight),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Visibility, contentDescription = null, tint = PurpleAccent, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("معاينة شكل الرسالة كما ستصل للعميل:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PurpleAccent)
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = CardWhite,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = livePreviewMessage,
                                        fontSize = 13.sp,
                                        color = PrimaryNavy,
                                        lineHeight = 20.sp,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Target Customer Selection Header
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "اختر العملاء المستهدفين (${selectedCustomerIds.size} محدد):",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryNavy
                            )

                            Row {
                                TextButton(onClick = onSelectAllDebtors) {
                                    Text("تحديد كافة المدينين", fontSize = 11.sp, color = RedDebt, fontWeight = FontWeight.Bold)
                                }
                                if (selectedCustomerIds.isNotEmpty()) {
                                    TextButton(onClick = onClearSelection) {
                                        Text("إلغاء", fontSize = 11.sp, color = TextSecondary)
                                    }
                                }
                            }
                        }
                    }

                    // Customer selector items
                    items(customers.filter { it.currentBalance > 0 }, key = { it.customer.id }) { item ->
                        val isSelected = selectedCustomerIds.contains(item.customer.id)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CardWhite,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onToggleSelect(item.customer.id) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { onToggleSelect(item.customer.id) }
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.customer.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryNavy)
                                    Text(item.customer.phone.ifBlank { "بدون هاتف" }, fontSize = 11.sp, color = TextSecondary)
                                }

                                Text(
                                    text = "${"%,.2f".format(Locale.US, item.currentBalance)} ${settings.currency}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RedDebt
                                )
                            }
                        }
                    }

                    // Send Action Buttons
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Direct Send via SMS
                            Button(
                                onClick = {
                                    requestAndSend {
                                        val targets = customers.filter { selectedCustomerIds.contains(it.customer.id) }
                                        targets.forEach { target ->
                                            val msg = templateText
                                                .replace("{name}", target.customer.name)
                                                .replace("{اسم_العميل}", target.customer.name)
                                                .replace("{balance}", "%,.2f".format(Locale.US, target.currentBalance))
                                                .replace("{المبلغ_المستحق}", "%,.2f".format(Locale.US, target.currentBalance))
                                                .replace("{currency}", settings.currency)
                                                .replace("{date}", SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date()))
                                                .replace("{store}", settings.storeName)

                                            onSendDirectSms(context, target.customer.phone, msg, target.customer.id, target.customer.name)
                                        }
                                    }
                                },
                                enabled = selectedCustomerIds.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("send_direct_sms_btn")
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "إرسال رسائل SMS المباشرة (${selectedCustomerIds.size} عميل)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Open in System SMS App
                            OutlinedButton(
                                onClick = {
                                    val target = customers.find { selectedCustomerIds.contains(it.customer.id) }
                                    if (target != null) {
                                        val msg = templateText
                                            .replace("{name}", target.customer.name)
                                            .replace("{اسم_العميل}", target.customer.name)
                                            .replace("{balance}", "%,.2f".format(Locale.US, target.currentBalance))
                                            .replace("{currency}", settings.currency)
                                            .replace("{store}", settings.storeName)

                                        onOpenSmsApp(context, target.customer.phone, msg)
                                    }
                                },
                                enabled = selectedCustomerIds.isNotEmpty(),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp), tint = SecondaryBlue)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("فتح في تطبيق الرسائل بالجهاز", fontSize = 13.sp, color = SecondaryBlue)
                            }
                        }
                    }
                }
            } else {
                // Sent SMS Logs Tab
                if (smsLogs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.MarkChatRead, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(60.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("لا توجد رسائل تذكير مرسلة حتى الآن", fontSize = 14.sp, color = TextSecondary)
                        }
                    }
                } else {
                    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale.getDefault()) }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(smsLogs, key = { it.id }) { log ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
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
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(GreenPaymentLight),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = GreenPayment, modifier = Modifier.size(16.dp))
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(log.customerName.ifBlank { "عميل" }, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryNavy)
                                                Text(log.recipientPhone, fontSize = 11.sp, color = TextSecondary)
                                            }
                                        }

                                        Text(dateFormat.format(Date(log.sentAt)), fontSize = 10.sp, color = TextSecondary)
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = BackgroundIce,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = log.messageText,
                                            fontSize = 12.sp,
                                            color = PrimaryNavy,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAiDrafterDialog) {
        val tones = listOf("ودود ولطيف", "رسمي ومهني", "حازم وتذكير عاجل", "إنذار نهائي قانوني")
        AlertDialog(
            onDismissRequest = { if (!isGeneratingAiMessage) showAiDrafterDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = SecondaryBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("صياغة رسالة بذكاء Gemini", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PrimaryNavy)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "اختر نبرة الرسالة المرغوبة وسيقوم نموذج Gemini 3.8 Flash بصياغة نص احترافي مخصص لحسابات متجرك:",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    tones.forEach { tone ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedTone == tone) TurquoiseLight else CardWhite,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (selectedTone == tone) SecondaryBlue else BackgroundIce
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedTone = tone }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(tone, fontSize = 13.sp, fontWeight = if (selectedTone == tone) FontWeight.Bold else FontWeight.Normal, color = PrimaryNavy)
                                RadioButton(selected = selectedTone == tone, onClick = { selectedTone = tone })
                            }
                        }
                    }

                    if (isGeneratingAiMessage) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = SecondaryBlue)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جاري استحضار وصياغة الرسالة من Gemini...", fontSize = 11.sp, color = SecondaryBlue)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isGeneratingAiMessage = true
                        coroutineScope.launch {
                            try {
                                val generated = aiService.generateCollectionMessage(
                                    customerName = previewTarget?.customer?.name ?: "{اسم_العميل}",
                                    amount = previewTarget?.currentBalance ?: 0.0,
                                    currency = settings.currency,
                                    storeName = settings.storeName,
                                    tone = selectedTone
                                )
                                templateText = generated
                                Toast.makeText(context, "تم تطبيق نص الرسالة الذكية بنجاح!", Toast.LENGTH_SHORT).show()
                                showAiDrafterDialog = false
                            } catch (e: Exception) {
                                Toast.makeText(context, "تعذر التوليد: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            } finally {
                                isGeneratingAiMessage = false
                            }
                        }
                    },
                    enabled = !isGeneratingAiMessage,
                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryBlue)
                ) {
                    Text(if (isGeneratingAiMessage) "جاري الصياغة..." else "تطبيق القالب الذكي")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAiDrafterDialog = false },
                    enabled = !isGeneratingAiMessage
                ) {
                    Text("إلغاء")
                }
            }
        )
    }
}
