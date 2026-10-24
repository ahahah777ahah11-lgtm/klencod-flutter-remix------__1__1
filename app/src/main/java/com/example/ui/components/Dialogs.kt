package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Customer
import com.example.data.model.CustomerWithBalance
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordDebtDialog(
    customers: List<CustomerWithBalance>,
    preSelectedCustomerId: Long? = null,
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (customerId: Long, amount: Double, category: String, invoiceNo: String, note: String) -> Unit
) {
    var selectedId by remember { mutableStateOf(preSelectedCustomerId ?: customers.firstOrNull()?.customer?.id ?: 0L) }
    var amountText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("دين بضاعة") }
    var invoiceNo by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    val categories = listOf("دين بضاعة", "فاتورة مؤجلة", "خدمات", "مشتريات", "عام")
    val selectedCustomer = customers.find { it.customer.id == selectedId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(RedDebtLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = RedDebt)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text("تسجيل دين جديد", fontWeight = FontWeight.Bold, color = PrimaryNavy, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Customer selector
                Text("اختر العميل:", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                ExposedDropdownMenuBox(
                    expanded = isDropdownExpanded,
                    onExpandedChange = { isDropdownExpanded = !isDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCustomer?.customer?.name ?: "اختر العميل...",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false }
                    ) {
                        customers.forEach { item ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(item.customer.name, fontWeight = FontWeight.SemiBold)
                                        Text("الرصيد: ${item.currentBalance} $currency", fontSize = 11.sp, color = if (item.currentBalance > 0) RedDebt else GreenPayment)
                                    }
                                },
                                onClick = {
                                    selectedId = item.customer.id
                                    isDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Amount
                Text("المبلغ ($currency):", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("debt_amount_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Category chips
                Text("نوع الدين / البضاعة:", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.take(3).forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Invoice Number
                Text("رقم الفاتورة / السند (اختياري):", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = invoiceNo,
                    onValueChange = { invoiceNo = it },
                    placeholder = { Text("مثال: INV-2026") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Notes
                Text("ملاحظات إضافية:", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text("اكتب أي تفاصيل أخرى...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (selectedId > 0 && amt > 0) {
                        onConfirm(selectedId, amt, category, invoiceNo, note)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = RedDebt),
                modifier = Modifier.testTag("confirm_record_debt_btn")
            ) {
                Text("حفظ الدين", color = CardWhite, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = TextSecondary)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordPaymentDialog(
    customers: List<CustomerWithBalance>,
    preSelectedCustomerId: Long? = null,
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (customerId: Long, amount: Double, paymentMethod: String, receiptNo: String, note: String) -> Unit
) {
    var selectedId by remember { mutableStateOf(preSelectedCustomerId ?: customers.firstOrNull()?.customer?.id ?: 0L) }
    var amountText by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("نقداً") }
    var receiptNo by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    val methods = listOf("نقداً", "حوالة مصرفية", "شيك", "تحويل بنكي")
    val selectedCustomer = customers.find { it.customer.id == selectedId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(GreenPaymentLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = GreenPayment)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text("تسجيل دفعة سداد", fontWeight = FontWeight.Bold, color = PrimaryNavy, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Customer selector
                Text("اختر العميل المسدد:", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                ExposedDropdownMenuBox(
                    expanded = isDropdownExpanded,
                    onExpandedChange = { isDropdownExpanded = !isDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCustomer?.customer?.name ?: "اختر العميل...",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false }
                    ) {
                        customers.forEach { item ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(item.customer.name, fontWeight = FontWeight.SemiBold)
                                        Text("الرصيد المتبقي: ${item.currentBalance} $currency", fontSize = 11.sp, color = RedDebt)
                                    }
                                },
                                onClick = {
                                    selectedId = item.customer.id
                                    isDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Amount
                Text("المبلغ المسدد ($currency):", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payment_amount_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Payment Method
                Text("طريقة السداد:", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    methods.forEach { m ->
                        FilterChip(
                            selected = paymentMethod == m,
                            onClick = { paymentMethod = m },
                            label = { Text(m, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Receipt Number
                Text("رقم سند القبض / الإيصال (اختياري):", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = receiptNo,
                    onValueChange = { receiptNo = it },
                    placeholder = { Text("مثال: REC-501") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Notes
                Text("ملاحظات إضافية:", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text("اكتب أي تفاصيل أخرى...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (selectedId > 0 && amt > 0) {
                        onConfirm(selectedId, amt, paymentMethod, receiptNo, note)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GreenPayment),
                modifier = Modifier.testTag("confirm_record_payment_btn")
            ) {
                Text("حفظ الدفعة", color = CardWhite, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = TextSecondary)
            }
        }
    )
}

@Composable
fun AddEditCustomerDialog(
    customer: Customer? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String, address: String, notes: String, creditLimit: Double, initialDebt: Double) -> Unit
) {
    var name by remember { mutableStateOf(customer?.name ?: "") }
    var phone by remember { mutableStateOf(customer?.phone ?: "") }
    var address by remember { mutableStateOf(customer?.address ?: "") }
    var notes by remember { mutableStateOf(customer?.notes ?: "") }
    var creditLimitText by remember { mutableStateOf(if ((customer?.creditLimit ?: 0.0) > 0) customer?.creditLimit.toString() else "") }
    var initialDebtText by remember { mutableStateOf("") }

    val isEditing = customer != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isEditing) "تعديل بيانات العميل" else "إضافة عميل جديد",
                fontWeight = FontWeight.Bold,
                color = PrimaryNavy,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Name
                Text("اسم العميل كامل *:", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("مثال: عبد الله أحمد باوزير") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_name_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Phone
                Text("رقم الهاتف (لرسائل SMS):", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    placeholder = { Text("05xxxxxxxx") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_phone_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Address
                Text("العنوان والمدينة:", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    placeholder = { Text("المدينة - الحي - الشارع") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Credit Limit
                Text("سقف الائتمان المسموح (اختياري):", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = creditLimitText,
                    onValueChange = { creditLimitText = it },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (!isEditing) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("دين سابق / رصيد افتتاحي (اختياري):", fontSize = 13.sp, color = RedDebt, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = initialDebtText,
                        onValueChange = { initialDebtText = it },
                        placeholder = { Text("0.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Notes
                Text("ملاحظات:", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = { Text("اكتب أي تفاصيل أخرى...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val limit = creditLimitText.toDoubleOrNull() ?: 0.0
                        val initial = initialDebtText.toDoubleOrNull() ?: 0.0
                        onConfirm(name, phone, address, notes, limit, initial)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy),
                modifier = Modifier.testTag("confirm_save_customer_btn")
            ) {
                Text("حفظ العميل", color = CardWhite, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = TextSecondary)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransferDialog(
    customers: List<CustomerWithBalance>,
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (company: String, transferNo: String, amount: Double, sender: String, customerId: Long?, note: String) -> Unit
) {
    var company by remember { mutableStateOf("الكريمي للصرافة") }
    var transferNo by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var senderName by remember { mutableStateOf("") }
    var selectedCustomerId by remember { mutableStateOf<Long?>(null) }
    var note by remember { mutableStateOf("") }
    var isCustomerDropdownExpanded by remember { mutableStateOf(false) }

    val companies = listOf("الكريمي للصرافة", "النجم للحوالات", "الراجحي", "بنك التضامن", "ويسترن يونيون", "أخرى")
    val selectedCustomer = customers.find { it.customer.id == selectedCustomerId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AmberLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = AmberAccent)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text("تسجيل حوالة واردة جديدة", fontWeight = FontWeight.Bold, color = PrimaryNavy, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Info banner
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = BackgroundIce,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "ملاحظة: تُسجل الحوالة كـ (قيد الانتظار) ويتم منع تكرار رقم الحوالة، ولن تُقيد كدفعة في حساب العميل إلا بعد تأكيد الاستلام.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Company
                Text("شركة التحويل / البنك *:", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    companies.take(3).forEach { comp ->
                        FilterChip(
                            selected = company == comp,
                            onClick = { company = comp },
                            label = { Text(comp, fontSize = 10.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Transfer Number (Unique)
                Text("رقم الحوالة (فريد لا يتكرر) *:", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = transferNo,
                    onValueChange = { transferNo = it },
                    placeholder = { Text("مثال: KR-123456") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transfer_no_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Amount
                Text("مبلغ الحوالة ($currency) *:", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transfer_amount_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Sender Name
                Text("اسم المرسل:", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = senderName,
                    onValueChange = { senderName = it },
                    placeholder = { Text("اسم الشخص المرسل للحوالة") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Link to Customer
                Text("ربط الحوالة بالعميل:", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                ExposedDropdownMenuBox(
                    expanded = isCustomerDropdownExpanded,
                    onExpandedChange = { isCustomerDropdownExpanded = !isCustomerDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCustomer?.customer?.name ?: "اختر العميل المعني بالحوالة...",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCustomerDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = isCustomerDropdownExpanded,
                        onDismissRequest = { isCustomerDropdownExpanded = false }
                    ) {
                        customers.forEach { item ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(item.customer.name, fontWeight = FontWeight.SemiBold)
                                        Text("الرصيد: ${item.currentBalance} $currency", fontSize = 11.sp, color = RedDebt)
                                    }
                                },
                                onClick = {
                                    selectedCustomerId = item.customer.id
                                    isCustomerDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Notes
                Text("ملاحظات:", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text("مثال: استلام عبر فرع شارع الستين") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (transferNo.isNotBlank() && amt > 0) {
                        onConfirm(company, transferNo, amt, senderName, selectedCustomerId, note)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AmberAccent),
                modifier = Modifier.testTag("confirm_save_transfer_btn")
            ) {
                Text("تسجيل الحوالة", color = CardWhite, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = TextSecondary)
            }
        }
    )
}

@Composable
fun QuickActionMenuDialog(
    onDismiss: () -> Unit,
    onRecordDebt: () -> Unit,
    onRecordPayment: () -> Unit,
    onAddCustomer: () -> Unit,
    onAddTransfer: () -> Unit,
    onSmsReminder: () -> Unit,
    onAiAssistant: () -> Unit = {}
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = CardWhite,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "العمليات المالية السريعة",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryNavy
                )
                Text(
                    text = "اختر الإجراء الذي تود تنفيذه فوراً",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(18.dp))

                QuickMenuOption(
                    title = "ذكاء Gemini ومستشار الديون",
                    subtitle = "استشارات مالية، ماسح الفواتير، وتحليل المخاطر",
                    icon = Icons.Default.AutoAwesome,
                    color = SecondaryBlue,
                    bgColor = TurquoiseLight,
                    onClick = {
                        onDismiss()
                        onAiAssistant()
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                QuickMenuOption(
                    title = "تسجيل دين جديد (على عميل)",
                    subtitle = "إضافة فاتورة أو مبلغ دين مستحق",
                    icon = Icons.Default.ArrowDownward,
                    color = RedDebt,
                    bgColor = RedDebtLight,
                    onClick = {
                        onDismiss()
                        onRecordDebt()
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                QuickMenuOption(
                    title = "تسجيل دفعة سداد (تحصيل)",
                    subtitle = "إيداع مبلغ وقيده كدفعة مسددة",
                    icon = Icons.Default.ArrowUpward,
                    color = GreenPayment,
                    bgColor = GreenPaymentLight,
                    onClick = {
                        onDismiss()
                        onRecordPayment()
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                QuickMenuOption(
                    title = "إضافة عميل جديد",
                    subtitle = "تسجيل حساب عميل مع رقم الهاتف",
                    icon = Icons.Default.PersonAdd,
                    color = SecondaryBlue,
                    bgColor = BackgroundIce,
                    onClick = {
                        onDismiss()
                        onAddCustomer()
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                QuickMenuOption(
                    title = "تسجيل حوالة واردة",
                    subtitle = "تسجيل رقم ومبلغ الحوالة ومنع تكرارها",
                    icon = Icons.Default.SwapHoriz,
                    color = AmberAccent,
                    bgColor = AmberLight,
                    onClick = {
                        onDismiss()
                        onAddTransfer()
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                QuickMenuOption(
                    title = "إرسال رسائل تذكير SMS",
                    subtitle = "مطالبة المدينين برسائل نصية ذكية",
                    icon = Icons.Default.Sms,
                    color = PurpleAccent,
                    bgColor = PurpleLight,
                    onClick = {
                        onDismiss()
                        onSmsReminder()
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("إغلاق النافذة", color = TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun QuickMenuOption(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    bgColor: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = BackgroundIce,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryNavy)
                Text(subtitle, fontSize = 11.sp, color = TextSecondary)
            }

            Icon(Icons.Default.ArrowBack, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
        }
    }
}
