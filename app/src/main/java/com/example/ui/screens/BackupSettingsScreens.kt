package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextAlign
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
import com.example.data.firebase.CloudBackupMetadata
import com.example.data.model.AppSettings
import com.google.firebase.auth.FirebaseUser
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.ui.components.AppTopBar
import com.example.ui.theme.*

@Composable
fun SettingsScreen(
    currentSettings: AppSettings,
    onSaveSettings: (AppSettings) -> Unit,
    onNavigateToBackup: () -> Unit,
    onBack: () -> Unit
) {
    var storeName by remember { mutableStateOf(currentSettings.storeName) }
    var ownerPhone by remember { mutableStateOf(currentSettings.ownerPhone) }
    var currency by remember { mutableStateOf(currentSettings.currency) }
    var defaultTemplate by remember { mutableStateOf(currentSettings.defaultSmsTemplate) }

    val popularCurrencies = listOf("ر.س", "د.ع", "ر.ي", "$", "ج.م", "د.ك", "درهم")

    Scaffold(
        topBar = {
            AppTopBar(
                title = "إعدادات التطبيق والمتجر",
                storeName = "تخصيص الاسم والعملة والقوالب",
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Store / App Name Card
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
                    Text("اسم المؤسسة أو المتجر:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PrimaryNavy)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = storeName,
                        onValueChange = { storeName = it },
                        placeholder = { Text("مثال: مؤسسة النجاح التجارية") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_store_name_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("رقم هاتف المتجر / المسؤول:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PrimaryNavy)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = ownerPhone,
                        onValueChange = { ownerPhone = it },
                        placeholder = { Text("05xxxxxxxx") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("رمز العملة المعتمدة:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PrimaryNavy)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = currency,
                        onValueChange = { currency = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        popularCurrencies.forEach { curr ->
                            FilterChip(
                                selected = currency == curr,
                                onClick = { currency = curr },
                                label = { Text(curr, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }

            // SMS Template Card
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
                    Text("قالب رسالة SMS الافتراضي:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PrimaryNavy)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = defaultTemplate,
                        onValueChange = { defaultTemplate = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        maxLines = 5
                    )
                }
            }

            // Save Settings Button
            Button(
                onClick = {
                    onSaveSettings(
                        currentSettings.copy(
                            storeName = storeName.ifBlank { "مدير الديون الذكي" },
                            ownerPhone = ownerPhone,
                            currency = currency.ifBlank { "ر.س" },
                            defaultSmsTemplate = defaultTemplate
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_settings_btn")
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("حفظ التغييرات", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            // Navigation to Backup & Restore Card
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
                    Text("النسخ الاحتياطي وإدارة البيانات:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PrimaryNavy)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("قم بتصدير قاعدة بياناتك محلياً أو استيرادها في أي وقت.", fontSize = 12.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = onNavigateToBackup,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(18.dp), tint = SecondaryBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("النسخ الاحتياطي والاستعادة وتوليد البيانات", color = SecondaryBlue)
                    }
                }
            }

            // App Information
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BackgroundIce),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("مدير الديون الذكي SMS - Smart Debt Manager", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PrimaryNavy)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("الإصدار 1.0.0 • قاعدة بيانات SQLite محلية بدون إنترنت", fontSize = 11.sp, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
fun BackupRestoreScreen(
    currentGoogleUser: FirebaseUser?,
    cloudBackups: List<CloudBackupMetadata>,
    isCloudLoading: Boolean,
    onExportJson: ((String) -> Unit) -> Unit,
    onImportJson: (String) -> Unit,
    onSeedDemoData: () -> Unit,
    onClearDatabase: () -> Unit,
    onSignInGoogle: () -> Unit,
    onSignOutGoogle: () -> Unit,
    onUploadCloudBackup: () -> Unit,
    onRefreshCloudBackups: () -> Unit,
    onRestoreCloudBackup: (CloudBackupMetadata) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var exportedJsonText by remember { mutableStateOf("") }
    var importInputText by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var backupToRestore by remember { mutableStateOf<CloudBackupMetadata?>(null) }
    val dateFormatter = remember { SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale.getDefault()) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "النسخ الاحتياطي السحابي والمحلي",
                storeName = "حفظ البيانات على سحابة Firebase ومحلياً",
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Section 1: Firebase Firestore Cloud Backup Card
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(TurquoiseLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CloudSync, contentDescription = null, tint = SecondaryBlue)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("النسخ السحابي (Firebase Firestore)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PrimaryNavy)
                                Text("حفظ مشفر ومزامنة حساباتك بأمان على السحابة", fontSize = 11.sp, color = TextSecondary)
                            }
                        }

                        if (isCloudLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = SecondaryBlue)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (currentGoogleUser == null) {
                        // User not logged in
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = BackgroundIce,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "قم بتسجيل الدخول باستخدام حساب Google لتفعيل النسخ الاحتياطي التلقائي واسترجاع بيانات متجرك في أي وقت من أي جهاز.",
                                    fontSize = 12.sp,
                                    color = TextPrimary,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = onSignInGoogle,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("firebase_google_signin_btn")
                                ) {
                                    Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("تسجيل الدخول باستخدام Google")
                                }
                            }
                        }
                    } else {
                        // Logged in
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = BackgroundIce,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(SecondaryBlue),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = CardWhite, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(currentGoogleUser.displayName ?: "مستخدم مسجل", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PrimaryNavy)
                                        Text(currentGoogleUser.email ?: "", fontSize = 11.sp, color = TextSecondary)
                                    }
                                }

                                TextButton(onClick = onSignOutGoogle) {
                                    Text("خروج", fontSize = 12.sp, color = RedDebt)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onUploadCloudBackup,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GreenPayment),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("firebase_upload_backup_btn")
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("رفع نسخة سحابية الآن", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = onRefreshCloudBackups,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("firebase_refresh_backups_btn")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "تحديث", modifier = Modifier.size(18.dp))
                            }
                        }

                        // List of Cloud Backups
                        if (cloudBackups.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Text("النسخ الاحتياطية السحابية المحفوظة في Firestore:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PrimaryNavy)
                            Spacer(modifier = Modifier.height(8.dp))

                            cloudBackups.forEach { backup ->
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = BackgroundIce),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(dateFormatter.format(Date(backup.timestamp)), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryNavy)
                                            Text("${backup.customerCount} عميل • ${backup.transactionCount} عملية مالية", fontSize = 11.sp, color = TextSecondary)
                                        }

                                        Button(
                                            onClick = { backupToRestore = backup },
                                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryBlue),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("استعادة", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        } else if (!isCloudLoading) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("لا توجد نسخ احتياطية سحابية بعد. اضغط 'رفع نسخة سحابية الآن' لإنشاء أول نسخة.", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                }
            }

            // Confirmation Dialog for Cloud Restore
            if (backupToRestore != null) {
                AlertDialog(
                    onDismissRequest = { backupToRestore = null },
                    title = { Text("تأكيد استعادة النسخة السحابية", fontWeight = FontWeight.Bold) },
                    text = {
                        Text("هل أنت متأكد من استعادة هذه النسخة السحابية المؤرخة في ${dateFormatter.format(Date(backupToRestore!!.timestamp))}؟ سيتم دمج وتحديث البيانات الحالية.")
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                onRestoreCloudBackup(backupToRestore!!)
                                backupToRestore = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryBlue)
                        ) {
                            Text("نعم، استعادة الآن")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { backupToRestore = null }) {
                            Text("إلغاء")
                        }
                    }
                )
            }
            // Export Card
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
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(GreenPaymentLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = GreenPayment)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("تصدير نسخة احتياطية", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PrimaryNavy)
                            Text("تصدير جميع العملاء والديون والمدفوعات كملف JSON", fontSize = 11.sp, color = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            onExportJson { json ->
                                exportedJsonText = json
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, json)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "مشاركة النسخة الاحتياطية")
                                shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(shareIntent)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تصدير ومشاركة النسخة الآن")
                    }

                    if (exportedJsonText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("DebtManagerBackup", exportedJsonText)
                                clipboard.setPrimaryClip(clip)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("نسخ كود JSON إلى الحافظة")
                        }
                    }
                }
            }

            // Import Card
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
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(TurquoiseLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, tint = TurquoiseAccent)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("استعادة نسخة احتياطية", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PrimaryNavy)
                            Text("استيراد بيانات العملاء والمعاملات من نص JSON سابق", fontSize = 11.sp, color = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { showImportDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("لصق واستعادة نسخة احتياطية")
                    }
                }
            }

            // Demo Data Generator Card
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
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(AmberLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = AmberAccent)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("توليد بيانات تجريبية واقعية", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PrimaryNavy)
                            Text("إضافة عينات عملاء وحوالات ومعاملات مالية للاختبار", fontSize = 11.sp, color = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = onSeedDemoData,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("توليد البيانات التجريبية الآن", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Wipe database
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
                    Text("مسح كافة البيانات", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = RedDebt)
                    Text("حذف جميع العملاء والديون والمدفوعات نهائياً وإعادة التطبيق لحالة البداية.", fontSize = 11.sp, color = RedDebt)
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { showClearDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = RedDebt),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("مسح وتفريغ قاعدة البيانات")
                    }
                }
            }
        }
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("لصق النسخة الاحتياطية", fontWeight = FontWeight.Bold, color = PrimaryNavy) },
            text = {
                Column {
                    Text("الصق محتوى JSON للنسخة الاحتياطية هنا:", fontSize = 12.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = importInputText,
                        onValueChange = { importInputText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        maxLines = 8
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importInputText.isNotBlank()) {
                            onImportJson(importInputText)
                            showImportDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy)
                ) {
                    Text("استعادة الآن")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("إلغاء", color = TextSecondary)
                }
            }
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("تحذير مسح البيانات", fontWeight = FontWeight.Bold, color = RedDebt) },
            text = { Text("هل أنت متأكد تماماً من تفريغ كافة البيانات؟ لا يمكن التراجع عن هذا الإجراء.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearDatabase()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedDebt)
                ) {
                    Text("نعم، امسح كل شيء", color = CardWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("إلغاء", color = TextSecondary)
                }
            }
        )
    }
}
