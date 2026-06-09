package com.example.agendei_pro.core.repository

import com.example.agendei_pro.core.model.Service
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import android.net.Uri
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ServiceRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val salonId: String? get() = auth.currentUser?.uid

    suspend fun uploadServiceImage(imageUri: Uri): Result<String> {
        val id = salonId ?: return Result.failure(Exception("Não autorizado"))
        val ref = storage.reference.child("salons/$id/services/${System.currentTimeMillis()}.jpg")
        return try {
            ref.putFile(imageUri).await()
            val url = ref.downloadUrl.await().toString()
            Result.success(url)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getServices(providedSalonId: String? = null): Flow<List<Service>> = callbackFlow {
        val id = providedSalonId ?: salonId
        if (id == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val subscription = firestore.collection("salons")
            .document(id)
            .collection("services")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val services = snapshot?.toObjects(Service::class.java) ?: emptyList()
                trySend(services)
            }

        awaitClose { subscription.remove() }
    }

    suspend fun addService(service: Service): Result<Unit> {
        val id = salonId ?: return Result.failure(Exception("Não autorizado"))
        return try {
            val docRef = firestore.collection("salons")
                .document(id)
                .collection("services")
                .document()
            
            val serviceWithId = service.copy(id = docRef.id)
            docRef.set(serviceWithId).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteService(serviceId: String): Result<Unit> {
        val id = salonId ?: return Result.failure(Exception("Não autorizado"))
        return try {
            firestore.collection("salons")
                .document(id)
                .collection("services")
                .document(serviceId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateService(service: Service): Result<Unit> {
        val id = salonId ?: return Result.failure(Exception("Não autorizado"))
        return try {
            firestore.collection("salons")
                .document(id)
                .collection("services")
                .document(service.id)
                .set(service)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
