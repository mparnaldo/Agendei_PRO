package com.example.agendei_pro.core.repository

import com.example.agendei_pro.core.model.UserBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ClientRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun getMyBindings(): List<UserBinding> {
        val user = auth.currentUser ?: return emptyList()
        return try {
            val snapshot = firestore.collection("user_bindings")
                .whereEqualTo("userId", user.uid)
                .get()
                .await()
            snapshot.toObjects(UserBinding::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun linkSalon(
        salonId: String, 
        salonName: String, 
        salonCode: String,
        logoUrl: String? = null,
        logoShape: String = "ROUND"
    ): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("Não logado"))
        val binding = UserBinding(
            userId = user.uid,
            salonId = salonId,
            salonName = salonName,
            salonCode = salonCode,
            isDefault = true,
            salonLogoUrl = logoUrl,
            salonLogoShape = logoShape
        )
        
        return try {
            firestore.collection("user_bindings")
                .document("${user.uid}_$salonId")
                .set(binding)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setDefaultSalon(salonId: String): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("Não logado"))
        return try {
            val batch = firestore.batch()
            val snapshots = firestore.collection("user_bindings")
                .whereEqualTo("userId", user.uid)
                .get()
                .await()
            
            for (doc in snapshots.documents) {
                val isTarget = doc.getString("salonId") == salonId
                batch.update(doc.reference, "isDefault", isTarget)
            }
            
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unlinkSalon(salonId: String): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("Não logado"))
        return try {
            firestore.collection("user_bindings")
                .document("${user.uid}_$salonId")
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
