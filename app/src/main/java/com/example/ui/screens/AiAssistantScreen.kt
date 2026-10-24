package com.example.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.AiOperationState
import com.example.ai.ScannedReceiptData
import com.example.data.model.AppSettings
import com.example.data.model.CustomerWithBalance
import com.example.ui.components.AppTopBar
import com.example.ui.theme.*
import kotlinx.coroutines.launch

data class ChatMessage(
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantScreen(
    settings: AppSettings,
    customers: List<CustomerWithBalance>,
    totalDebts: Double,
    totalPayments: Double,
    chatMessages: List<ChatMessage>,
    isAdvisorGenerating: Boolean,
    analysisState: AiOperationState<String>,
    scannedReceiptState: AiOperationState<ScannedReceiptData>,
    onSendMessage: (String) -> Unit,
    onRunAnalysis: () -> Unit,
    onScanReceipt: (Bitmap) -> Unit,
    onApplyScannedData: (ScannedReceiptData) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("المستشار المالي", "ماسح الفواتير", "تحليل المحفظة")

    Scaffold(
        topBar = {
            AppTopBar(
                title = "الذكاء الاصطناعي (Gemini 3.8)",
                storeName = "مساعدك المحاسبي والمالي الذكي",
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
            // Tab Selector
            Surface(
                color = CardWhite,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = CardWhite,
                    contentColor = PrimaryNavy
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 13.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            icon = {
                                when (index) {
                                    0 -> Icon(Icons.Default.SmartToy, contentDescription = null, modifier = Modifier.size(20.dp))
                                    1 -> Icon(Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(20.dp))
                                    else -> Icon(Icons.Default.Insights, contentDescription = null, modifier = Modifier.size(20.dp))
                                }
                            }
                        )
                    }
                }
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    0 -> AdvisorChatTab(
                        messages = chatMessages,
                        isGenerating = isAdvisorGenerating,
                        onSendMessage = onSendMessage
                    )
                    1 -> ReceiptScannerTab(
                        scannedReceiptState = scannedReceiptState,
                        currency = settings.currency,
                        onScanReceipt = onScanReceipt,
                        onApplyData = onApplyScannedData
                    )
                    2 -> PortfolioAnalysisTab(
                        analysisState = analysisState,
                        totalDebts = totalDebts,
                        totalPayments = totalPayments,
                        customerCount = customers.size,
                        currency = settings.currency,
                        onRunAnalysis = onRunAnalysis,
                        onShareReport = { reportText ->
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, reportText)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "مشاركة التقرير المالي الذكي")
                            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(shareIntent)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AdvisorChatTab(
    messages: List<ChatMessage>,
    isGenerating: Boolean,
    onSendMessage: (String) -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val quickPills = listOf(
        "كيف أتعامل مع عميل يماطل في السداد؟",
        "ما هي أفضل نسبة ديون آمنة لرأس المال؟",
        "صيغة رسالة تذكير مهذبة جداً",
        "طريقة إعادة جدولة ديون عميل متعثر"
    )

    LaunchedEffect(messages.size, isGenerating) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Quick Suggestion Pills
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickPills) { pill ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CardWhite,
                    border = androidx.compose.foundation.BorderStroke(1.dp, TurquoiseLight),
                    modifier = Modifier.clickable {
                        onSendMessage(pill)
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = SecondaryBlue, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(pill, fontSize = 11.sp, color = PrimaryNavy, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        // Chat Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(messages) { msg ->
                ChatBubble(message = msg)
            }

            if (isGenerating) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = SecondaryBlue)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("جاري استحضار الإجابة بواسطة Gemini 3.8...", fontSize = 12.sp, color = TextSecondary)
                    }
                }
            }
        }

        // Bottom Input Row
        Surface(
            color = CardWhite,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("اطرح سؤالك المالي على Gemini...", fontSize = 13.sp) },
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_advisor_chat_input"),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        val text = inputText.trim()
                        if (text.isNotBlank()) {
                            onSendMessage(text)
                            inputText = ""
                        }
                    },
                    enabled = inputText.isNotBlank() && !isGenerating,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = SecondaryBlue,
                        disabledContainerColor = BackgroundIce
                    ),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("ai_advisor_send_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "إرسال",
                        tint = CardWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.isUser
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bgColor = if (isUser) PrimaryNavy else CardWhite
    val textColor = if (isUser) CardWhite else TextPrimary
    val shape = if (isUser) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            if (!isUser) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(TurquoiseLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = SecondaryBlue, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Surface(
                shape = shape,
                color = bgColor,
                shadowElevation = if (isUser) 0.dp else 1.dp,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = message.text,
                        fontSize = 13.sp,
                        color = textColor,
                        lineHeight = 20.sp
                    )
                }
            }

            if (isUser) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(SecondaryBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = CardWhite, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun ReceiptScannerTab(
    scannedReceiptState: AiOperationState<ScannedReceiptData>,
    currency: String,
    onScanReceipt: (Bitmap) -> Unit,
    onApplyData: (ScannedReceiptData) -> Unit
) {
    val context = LocalContext.current
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Modern zero-permission Android Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT < 28) {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                } else {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                }
                selectedBitmap = bitmap
                onScanReceipt(bitmap)
            } catch (e: Exception) {
                // Ignore decoding error
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.DocumentScanner,
                        contentDescription = null,
                        tint = SecondaryBlue,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ماسح الفواتير والسندات الذكي",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryNavy
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "التقط أو اختر صورة سند قبض أو فاتورة مبيعات، وسيقوم نموذج Gemini 3.8 Flash باستخراج الاسم والمبلغ ونوع السند آلياً.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_pick_receipt_btn")
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("اختيار صورة سند / فاتورة من المعرض")
                    }
                }
            }
        }

        // Preview Image if selected
        if (selectedBitmap != null) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("معاينة المستند المختار:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryNavy)
                        Spacer(modifier = Modifier.height(8.dp))
                        Image(
                            bitmap = selectedBitmap!!.asImageBitmap(),
                            contentDescription = "المستند المختار",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                }
            }
        }

        // Status or Result
        when (scannedReceiptState) {
            is AiOperationState.Loading -> {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardWhite),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = SecondaryBlue)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("جاري فحص المستند واستخراج البيانات المالية بواسطة Gemini...", fontSize = 13.sp, color = PrimaryNavy, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            is AiOperationState.Error -> {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = RedDebtLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text("تعذر استخراج البيانات من الصورة:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = RedDebt)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(scannedReceiptState.errorMessage, fontSize = 12.sp, color = TextPrimary)
                        }
                    }
                }
            }

            is AiOperationState.Success -> {
                val data = scannedReceiptState.data
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
                                    text = "تم استخراج البيانات بنجاح ✅",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GreenPayment
                                )

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (data.transactionType == "DEBT") RedDebtLight else GreenPaymentLight
                                ) {
                                    Text(
                                        text = if (data.transactionType == "DEBT") "سند دين / فاتورة آجلة" else "سند قبض / سداد",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (data.transactionType == "DEBT") RedDebt else GreenPayment,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            ExtractedInfoRow(label = "اسم العميل المستخرج:", value = data.customerName.ifBlank { "غير محدد" })
                            ExtractedInfoRow(label = "المبلغ:", value = "${data.amount} $currency")
                            if (data.invoiceOrReceiptNo.isNotBlank()) {
                                ExtractedInfoRow(label = "رقم الفاتورة/السند:", value = data.invoiceOrReceiptNo)
                            }
                            if (data.category.isNotBlank()) {
                                ExtractedInfoRow(label = "التصنيف:", value = data.category)
                            }
                            if (data.notes.isNotBlank()) {
                                ExtractedInfoRow(label = "ملاحظات:", value = data.notes)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { onApplyData(data) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (data.transactionType == "DEBT") RedDebt else GreenPayment
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("ai_apply_scanned_receipt_btn")
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (data.transactionType == "DEBT") "تسجيل هذا الدين للعميل في الحسابات" else "تسجيل هذه الدفعة المسددة في الحسابات"
                                )
                            }
                        }
                    }
                }
            }

            else -> {}
        }
    }
}

@Composable
fun ExtractedInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = TextSecondary)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PrimaryNavy)
    }
}

@Composable
fun PortfolioAnalysisTab(
    analysisState: AiOperationState<String>,
    totalDebts: Double,
    totalPayments: Double,
    customerCount: Int,
    currency: String,
    onRunAnalysis: () -> Unit,
    onShareReport: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(TurquoiseLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Analytics, contentDescription = null, tint = SecondaryBlue)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("تحليل الديون والمخاطر المالي", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PrimaryNavy)
                            Text("تقييم مخاطر التعثر ومعدل التحصيل عبر Gemini 3.8 Flash", fontSize = 11.sp, color = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = BackgroundIce,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("إجمالي الديون", fontSize = 11.sp, color = TextSecondary)
                                Text("${totalDebts} $currency", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = RedDebt)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = BackgroundIce,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("المتحصلات", fontSize = 11.sp, color = TextSecondary)
                                Text("${totalPayments} $currency", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GreenPayment)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = BackgroundIce,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("العملاء", fontSize = 11.sp, color = TextSecondary)
                                Text("$customerCount عميل", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PrimaryNavy)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onRunAnalysis,
                        enabled = analysisState !is AiOperationState.Loading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_run_analysis_btn")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (analysisState is AiOperationState.Loading) "جاري التحليل المالي..." else "تشغيل فحص المخاطر والتحليل الذكي الآن")
                    }
                }
            }
        }

        when (analysisState) {
            is AiOperationState.Loading -> {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardWhite),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = SecondaryBlue)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("يقوم نموذج Gemini بتحليل توزيع الديون وقياس درجات المخاطر الائتمانية...", fontSize = 12.sp, color = TextSecondary, textAlign = TextAlign.Center)
                        }
                    }
                }
            }

            is AiOperationState.Error -> {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = RedDebtLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("حدث خطأ أثناء إجراء التحليل:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = RedDebt)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(analysisState.errorMessage, fontSize = 12.sp, color = TextPrimary)
                        }
                    }
                }
            }

            is AiOperationState.Success -> {
                val reportText = analysisState.data
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("📊 التقرير المالي الصادر من Gemini", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PrimaryNavy)
                                IconButton(onClick = { onShareReport(reportText) }) {
                                    Icon(Icons.Default.Share, contentDescription = "مشاركة", tint = SecondaryBlue)
                                }
                            }

                            Divider(modifier = Modifier.padding(vertical = 10.dp), color = BackgroundIce)

                            Text(
                                text = reportText,
                                fontSize = 13.sp,
                                color = TextPrimary,
                                lineHeight = 22.sp
                            )
                        }
                    }
                }
            }

            else -> {}
        }
    }
}
