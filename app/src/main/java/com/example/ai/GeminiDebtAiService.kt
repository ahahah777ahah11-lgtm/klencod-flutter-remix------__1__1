package com.example.ai

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "GeminiDebtAiService"

data class ScannedReceiptData(
    val transactionType: String, // "DEBT" or "PAYMENT"
    val customerName: String,
    val amount: Double,
    val invoiceOrReceiptNo: String,
    val dateString: String,
    val category: String,
    val notes: String
)

sealed interface AiOperationState<out T> {
    data object Idle : AiOperationState<Nothing>
    data object Loading : AiOperationState<Nothing>
    data class Streaming(val partialText: String) : AiOperationState<Nothing>
    data class Success<T>(val data: T) : AiOperationState<T>
    data class Error(val errorMessage: String) : AiOperationState<Nothing>
}

class GeminiDebtAiService {

    // Mandated: Initialize generative model lazily with gemini-3.8-flash
    private val textModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash"
        )
    }

    private val jsonModel by lazy {
        val config = generationConfig {
            responseMimeType = "application/json"
        }
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = config
        )
    }

    /**
     * Generate customized collection reminder messages using Gemini 3.8 Flash
     */
    suspend fun generateCollectionMessage(
        customerName: String,
        amount: Double,
        currency: String,
        storeName: String,
        tone: String,
        customInstruction: String = ""
    ): String {
        return withTimeout(60_000L) {
            try {
                val prompt = buildString {
                    append("أنت خبير في العلاقات التجارية وإدارة الحسابات والديون في العالم العربي.\n")
                    append("قم بصياغة رسالة تذكير بسداد دين أو دفعة مالية باللغة العربية تكون ملائمة للإرسال عبر SMS أو WhatsApp.\n")
                    append("معلومات العملية:\n")
                    append("- اسم العميل: $customerName\n")
                    append("- المبلغ المطلوب: $amount $currency\n")
                    append("- اسم المتجر أو الدائن: $storeName\n")
                    append("- النبرة المطلوبة: $tone (خيارات النبرة: ودود ولطيف / رسمي ومهني / حازم وتذكير عاجل / إنذار نهائي قانوني)\n")
                    if (customInstruction.isNotBlank()) {
                        append("- تعليمات إضافية: $customInstruction\n")
                    }
                    append("\nالمطلوب: كتابة نص الرسالة فقط بدون أي مقدمات أو شروحات، بحيث تكون جاهزة للنسخ والإرسال مباشرة.")
                }

                val response = textModel.generateContent(prompt)
                val text = response.text
                if (!text.isNullOrBlank()) {
                    text.trim()
                } else {
                    throw IllegalStateException("لم يُرجع الذكاء الاصطناعي أي نص.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Message drafting timed out", e)
                throw e
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate reminder message", e)
                throw e
            }
        }
    }

    /**
     * Multimodal OCR: Scans a receipt, paper voucher, or invoice photo and extracts structured financial data
     */
    suspend fun scanReceiptOrInvoice(bitmap: Bitmap): ScannedReceiptData {
        return withTimeout(60_000L) {
            try {
                val prompt = """
                    أنت خبير محاسبي متخصص في فحص المستندات المالية، الفواتير، وسندات القبض والصرف، وإيصالات الحوالات البنكية.
                    قم بقراءة وتحليل هذه الصورة واستخراج البيانات المالية بدقة بصيغة JSON حصراً بالبنية التالية:
                    {
                      "transactionType": "DEBT" أو "PAYMENT" (إذا كان سند قبض أو إيصال سداد/تحويل اختر PAYMENT، إذا كانت فاتورة مبيعات آجلة أو سند دين اختر DEBT),
                      "customerName": "اسم العميل أو المستلم/المرسل المستخرج من الصورة",
                      "amount": رقم المبلغ كقيمة رقمية عائمة بدون نصوص مثلا 150.0,
                      "invoiceOrReceiptNo": "رقم الفاتورة أو رقم السند أو رقم الحوالة إن وجد",
                      "dateString": "التاريخ المكتوب في السند إن وجد",
                      "category": "تصنيف العملية مثلا مبيعات، سداد نقدي، حوالة بنكية، بضاعة",
                      "notes": "ملاحظات أو بيان السند"
                    }
                    ملاحظة: إذا لم يظهر اسم العميل بوضوح ضع اسم تقريبي أو "عميل نقدي". أرجع كائن JSON الصالح فقط.
                """.trimIndent()

                val response = jsonModel.generateContent(
                    content {
                        image(bitmap)
                        text(prompt)
                    }
                )

                val jsonStr = response.text ?: throw IllegalStateException("لم يتم استخراج نص من الفاتورة.")
                val json = JSONObject(jsonStr)

                ScannedReceiptData(
                    transactionType = json.optString("transactionType", "PAYMENT").uppercase(),
                    customerName = json.optString("customerName", "عميل"),
                    amount = json.optDouble("amount", 0.0),
                    invoiceOrReceiptNo = json.optString("invoiceOrReceiptNo", ""),
                    dateString = json.optString("dateString", ""),
                    category = json.optString("category", "سند"),
                    notes = json.optString("notes", "")
                )
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Receipt scan timed out", e)
                throw e
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Receipt scanning failed", e)
                throw e
            }
        }
    }

    /**
     * In-depth Financial Health & Risk Analysis of debts
     */
    suspend fun analyzeFinancialPortfolio(
        totalDebts: Double,
        totalPayments: Double,
        customerCount: Int,
        debtorsSummary: String,
        currency: String
    ): String {
        return withTimeout(60_000L) {
            try {
                val prompt = """
                    أنت مستشار مالي ومحلل مخاطر ائتمانية خبير للشركات الصغيرة والمتوسطة والمتاجر في الشرق الأوسط.
                    قم بتحليل المحفظة المالية والديون التالية وتقديم تقرير تحليلي ذكي وشامل باللغة العربية:
                    
                    البيانات المالية:
                    - إجمالي الديون المستحقة على العملاء: $totalDebts $currency
                    - إجمالي المبالغ المحصلة: $totalPayments $currency
                    - صافي الرصيد القائم: ${totalDebts - totalPayments} $currency
                    - عدد العملاء الإجمالي: $customerCount
                    
                    تفاصيل العملاء المدينين وأرصدتهم:
                    $debtorsSummary
                    
                    المطلوب في التقرير:
                    1. تقييم الملاءة المالية ونسبة التحصيل الحالية.
                    2. تصنيف مستوى المخاطر (منخفض / متوسط / مرتفع) مع التبرير.
                    3. تحديد العملاء ذوي الحجم الأكبر والأعلى مخاطرة (مبدأ باريتو 80/20).
                    4. خطة عمل وتوصيات فورية لتحصيل الديون وزيادة السيولة النقدية دون خسارة العملاء.
                    
                    صغ التقرير بتنسيق جذاب وواضح مع عناوين ونقاط بارزة وموجزة.
                """.trimIndent()

                val response = textModel.generateContent(prompt)
                response.text ?: "لا توجد تفاصيل متاحة."
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Financial analysis timed out", e)
                throw e
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Financial analysis failed", e)
                throw e
            }
        }
    }

    /**
     * Interactive Chat with Financial Advisor using streaming
     */
    fun chatWithAdvisorStream(userMessage: String, contextInfo: String = ""): Flow<String> = flow {
        withTimeout(60_000L) {
            try {
                val prompt = buildString {
                    append("أنت 'مستشار الديون الذكي' المدعوم بنموذج Google Gemini 3.8 Flash، ومساعد مالي متخصص لأصحاب المتاجر والأنشطة التجارية في العالم العربي.\n")
                    if (contextInfo.isNotBlank()) {
                        append("سياق بيانات المتجر الحالية:\n$contextInfo\n\n")
                    }
                    append("سؤال التاجر:\n$userMessage\n\n")
                    append("أجب بدقة ومهنية عالية وبلهجة تجارية راقية ومفيدة، وقدم نصائح عملية ومباشرة.")
                }

                textModel.generateContentStream(prompt).collect { chunk ->
                    val text = chunk.text
                    if (!text.isNullOrEmpty()) {
                        emit(text)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Streaming chat failed", e)
                throw e
            }
        }
    }
}
