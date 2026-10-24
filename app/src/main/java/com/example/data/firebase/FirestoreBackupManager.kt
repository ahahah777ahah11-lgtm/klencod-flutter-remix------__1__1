package com.example.data.firebase

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

private const val TAG = "FirestoreBackupManager"

data class CloudBackupMetadata(
    val id: String = "",
    val userId: String = "",
    val storeName: String = "",
    val customerCount: Int = 0,
    val transactionCount: Int = 0,
    val timestamp: Long = 0L,
    val dataJson: String = ""
)

class FirestoreBackupManager(private val context: Context) {

    // Mandated: Always load database ID from R.string.firestore_database_id
    val firestore: FirebaseFirestore by lazy {
        val databaseId = context.getString(R.string.firestore_database_id)
        FirebaseFirestore.getInstance(databaseId)
    }

    val auth: FirebaseAuth by lazy {
        Firebase.auth
    }

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    /**
     * Google Sign-In using Jetpack Credential Manager
     */
    suspend fun signInWithGoogle(): Result<FirebaseUser> {
        return try {
            val serverClientId = context.getString(R.string.default_web_client_id)
            val signInOption = GetSignInWithGoogleOption.Builder(serverClientId)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInOption)
                .build()

            val credentialManager = CredentialManager.create(context)
            val result = credentialManager.getCredential(context = context, request = request)

            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(authCredential).await()
                val user = authResult.user
                if (user != null) {
                    // Update user profile in Firestore
                    val userDoc = firestore.collection("users").document(user.uid)
                    userDoc.set(
                        mapOf(
                            "email" to (user.email ?: ""),
                            "displayName" to (user.displayName ?: ""),
                            "updatedAt" to System.currentTimeMillis()
                        )
                    ).await()
                    Result.success(user)
                } else {
                    Result.failure(Exception("فشل تسجيل الدخول بحساب Google."))
                }
            } else {
                Result.failure(Exception("نوع بيانات الاعتماد غير مدعوم."))
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Sign in was cancelled by the user", e)
            Result.failure(Exception("تم إلغاء عملية تسجيل الدخول."))
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed", e)
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
    }

    /**
     * Upload an isolated cloud backup snapshot to Firestore
     */
    suspend fun uploadBackupToFirestore(
        backupJson: String,
        storeName: String,
        customerCount: Int,
        transactionCount: Int
    ): Result<String> {
        val user = auth.currentUser
        if (user == null) {
            return Result.failure(Exception("يجب تسجيل الدخول باستخدام Google أولاً لإجراء النسخ السحابي."))
        }

        return try {
            val backupDoc = firestore.collection("backups").document()
            val backupData = mapOf(
                "userId" to user.uid,
                "storeName" to storeName,
                "customerCount" to customerCount,
                "transactionCount" to transactionCount,
                "timestamp" to System.currentTimeMillis(),
                "dataJson" to backupJson,
                "createdAt" to FieldValue.serverTimestamp()
            )

            backupDoc.set(backupData).await()
            Result.success(backupDoc.id)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload backup to Firestore", e)
            Result.failure(e)
        }
    }

    /**
     * Fetch user's cloud backups from Firestore
     */
    suspend fun fetchCloudBackups(): Result<List<CloudBackupMetadata>> {
        val user = auth.currentUser
        if (user == null) {
            return Result.failure(Exception("يجب تسجيل الدخول باستخدام Google لعرض النسخ السحابية."))
        }

        return try {
            val querySnapshot = firestore.collection("backups")
                .whereEqualTo("userId", user.uid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            val list = querySnapshot.documents.mapNotNull { doc ->
                val id = doc.id
                val userId = doc.getString("userId") ?: return@mapNotNull null
                val store = doc.getString("storeName") ?: "متجر"
                val cCount = (doc.getLong("customerCount") ?: 0L).toInt()
                val tCount = (doc.getLong("transactionCount") ?: 0L).toInt()
                val timestamp = doc.getLong("timestamp") ?: 0L
                val dataJson = doc.getString("dataJson") ?: ""

                CloudBackupMetadata(
                    id = id,
                    userId = userId,
                    storeName = store,
                    customerCount = cCount,
                    transactionCount = tCount,
                    timestamp = timestamp,
                    dataJson = dataJson
                )
            }
            Result.success(list)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch cloud backups", e)
            Result.failure(e)
        }
    }
}
