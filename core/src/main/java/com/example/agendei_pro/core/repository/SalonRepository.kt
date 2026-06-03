package com.example.agendei_pro.core.repository

import com.example.agendei_pro.core.model.Salon
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import android.net.Uri
import kotlinx.coroutines.tasks.await
import java.util.Date
import kotlin.coroutines.resume

class SalonRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    suspend fun uploadLogo(imageUri: Uri): Result<String> {
        val user = auth.currentUser ?: return Result.failure(Exception("Não logado"))
        val ref = storage.reference.child("logos/${user.uid}.jpg")
        return try {
            ref.putFile(imageUri).await()
            val url = ref.downloadUrl.await().toString()
            firestore.collection("salons").document(user.uid).update("logoUrl", url).await()
            Result.success(url)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadLogoWithProgress(imageUri: Uri, onProgress: (Int) -> Unit): Result<String> {
        val user = auth.currentUser ?: return Result.failure(Exception("Não logado"))
        val ref = storage.reference.child("logos/${user.uid}.jpg")
        return try {
            val uploadTask = ref.putFile(imageUri)
            val uploadResult = kotlinx.coroutines.suspendCancellableCoroutine<Result<Unit>> { continuation ->
                val progressListener = com.google.firebase.storage.OnProgressListener<com.google.firebase.storage.UploadTask.TaskSnapshot> { snapshot ->
                    val progress = if (snapshot.totalByteCount > 0) {
                        (100 * snapshot.bytesTransferred / snapshot.totalByteCount).toInt()
                    } else 0
                    onProgress(progress)
                }
                uploadTask.addOnProgressListener(progressListener)
                uploadTask.addOnSuccessListener {
                    continuation.resume(Result.success(Unit))
                }.addOnFailureListener { e ->
                    continuation.resume(Result.failure(e))
                }
                continuation.invokeOnCancellation {
                    uploadTask.cancel()
                }
            }
            if (uploadResult.isFailure) {
                return Result.failure(uploadResult.exceptionOrNull() ?: Exception("Upload falhou"))
            }
            val url = ref.downloadUrl.await().toString()
            firestore.collection("salons").document(user.uid).update("logoUrl", url).await()
            Result.success(url)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeLogo(): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("Não logado"))
        return try {
            storage.reference.child("logos/${user.uid}.jpg").delete().await()
            firestore.collection("salons").document(user.uid).update("logoUrl", null).await()
            Result.success(Unit)
        } catch (e: Exception) {
            firestore.collection("salons").document(user.uid).update("logoUrl", null).await()
            Result.success(Unit)
        }
    }

    suspend fun registerSalon(name: String, address: String, phone: String, segment: String): Result<Salon> {
        val user = auth.currentUser ?: return Result.failure(Exception("Não logado"))
        val numbers = (1000..9999).random().toString()
        val letters = ('A'..'Z').shuffled().take(2).joinToString("")
        val generatedCode = "PRO-$numbers$letters"
        
        val newSalon = Salon(
            id = user.uid,
            name = name,
            ownerUid = user.uid,
            code = generatedCode,
            address = address,
            phoneNumber = phone,
            trialStartDate = Date(),
            isSubscribed = false,
            segment = segment
        )

        return try {
            firestore.collection("salons").document(user.uid).set(newSalon).await()
            Result.success(newSalon)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSalon(): Salon? {
        val user = auth.currentUser ?: return null
        return try {
            val snapshot = firestore.collection("salons").document(user.uid).get().await()
            snapshot.toObject(Salon::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getSalonById(id: String): Salon? {
        return try {
            val snapshot = firestore.collection("salons").document(id).get().await()
            snapshot.toObject(Salon::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getSalonByCode(code: String): Salon? {
        return try {
            val snapshot = firestore.collection("salons").whereEqualTo("code", code).limit(1).get().await()
            if (snapshot.isEmpty) null else snapshot.documents[0].toObject(Salon::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun getRemainingTrialDays(salon: Salon): Int {
        if (salon.isSubscribed) return 999
        val startDate = salon.trialStartDate ?: return 0
        val now = Date()
        val diffInMillies = now.time - startDate.time
        val diffInDays = (diffInMillies / (1000 * 60 * 60 * 24)).toInt()
        val remaining = 10 - diffInDays
        return if (remaining < 0) 0 else remaining
    }

    suspend fun updateSubscriptionStatus(isActive: Boolean): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("Não logado"))
        return try {
            firestore.collection("salons").document(user.uid).update("isSubscribed", isActive).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Funções de Admin
    suspend fun getAllSalons(): List<Salon> {
        return try {
            val snapshot = firestore.collection("salons").orderBy("trialStartDate").get().await()
            snapshot.toObjects(Salon::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun giftPremium(salonId: String): Result<Unit> {
        return try {
            firestore.collection("salons").document(salonId).update("isSubscribed", true).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateSalonSettings(
        name: String,
        opening: String, 
        closing: String,
        breakStart: String,
        breakEnd: String,
        days: List<Int>,
        autoAccept: Boolean,
        logoShape: String,
        segment: String,
        hasLoyalty: Boolean,
        loyaltyRequired: Int,
        loyaltyReward: String
    ): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("Não logado"))
        return try {
            firestore.collection("salons").document(user.uid).update(mapOf(
                "name" to name,
                "openingTime" to opening,
                "closingTime" to closing,
                "breakStart" to breakStart,
                "breakEnd" to breakEnd,
                "workingDays" to days,
                "autoAccept" to autoAccept,
                "logoShape" to logoShape,
                "segment" to segment,
                "hasLoyaltyProgram" to hasLoyalty,
                "loyaltyRequiredServices" to loyaltyRequired,
                "loyaltyRewardDescription" to loyaltyReward
            )).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
