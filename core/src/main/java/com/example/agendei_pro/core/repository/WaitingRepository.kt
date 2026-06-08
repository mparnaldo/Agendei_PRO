package com.example.agendei_pro.core.repository

import com.example.agendei_pro.core.model.WaitingEntry
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.*

class WaitingRepository {
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun createWaitingEntry(entry: WaitingEntry): Result<Unit> = try {
        val docRef = firestore.collection("waiting_list").document()
        val finalEntry = entry.copy(id = docRef.id, createdAt = Date())
        firestore.collection("waiting_list").document(docRef.id).set(finalEntry).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun getWaitingListForSalon(salonId: String): Flow<List<WaitingEntry>> = callbackFlow {
        val subscription = firestore.collection("waiting_list")
            .whereEqualTo("salonId", salonId)
            .whereEqualTo("status", "WAITING")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val list = snapshot?.toObjects(WaitingEntry::class.java) ?: emptyList()
                trySend(list.sortedBy { it.date ?: Date() })
            }
        awaitClose { subscription.remove() }
    }

    fun getWaitingListForClient(clientUid: String): Flow<List<WaitingEntry>> = callbackFlow {
        val subscription = firestore.collection("waiting_list")
            .whereEqualTo("clientUid", clientUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val list = snapshot?.toObjects(WaitingEntry::class.java) ?: emptyList()
                trySend(list.sortedByDescending { it.date ?: Date() })
            }
        awaitClose { subscription.remove() }
    }

    suspend fun updateWaitingStatus(id: String, status: String): Result<Unit> = try {
        firestore.collection("waiting_list").document(id).update("status", status).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun deleteWaitingEntry(id: String): Result<Unit> = try {
        firestore.collection("waiting_list").document(id).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
