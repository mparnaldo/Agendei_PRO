package com.example.agendei_pro.core.repository

import com.example.agendei_pro.core.model.Appointment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.*

class AppointmentRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun getAppointmentsForDay(date: Date, salonId: String): Flow<List<Appointment>> = callbackFlow {
        // Normaliza a data para o inicio e fim do dia no fuso LOCAL
        val cal = Calendar.getInstance().apply { 
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.time
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        val end = cal.time

        val subscription = firestore.collection("appointments")
            .whereEqualTo("salonId", salonId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val list = snapshot?.toObjects(Appointment::class.java) ?: emptyList()
                val filtered = list.filter { it.date != null && !it.date!!.before(start) && !it.date!!.after(end) }
                trySend(filtered.sortedBy { it.date })
            }
        awaitClose { subscription.remove() }
    }

    fun getAllSalonAppointments(salonId: String): Flow<List<Appointment>> = callbackFlow {
        val subscription = firestore.collection("appointments")
            .whereEqualTo("salonId", salonId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val list = snapshot?.toObjects(Appointment::class.java) ?: emptyList()
                trySend(list)
            }
        awaitClose { subscription.remove() }
    }

    fun getMyAppointments(): Flow<List<Appointment>> = callbackFlow {
        val user = auth.currentUser ?: run { trySend(emptyList()); close(); return@callbackFlow }
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
        }
        val subscription = firestore.collection("appointments")
            .whereEqualTo("clientUid", user.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val list = snapshot?.toObjects(Appointment::class.java) ?: emptyList()
                // Limpa o lixo do passado
                val filtered = list.filter { it.date != null && !it.date!!.before(cal.time) }
                trySend(filtered.sortedBy { it.date })
            }
        awaitClose { subscription.remove() }
    }

    fun getMyCompleteAppointments(): Flow<List<Appointment>> = callbackFlow {
        val user = auth.currentUser ?: run { trySend(emptyList()); close(); return@callbackFlow }
        val subscription = firestore.collection("appointments")
            .whereEqualTo("clientUid", user.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val list = snapshot?.toObjects(Appointment::class.java) ?: emptyList()
                trySend(list.sortedByDescending { it.date ?: Date(0) })
            }
        awaitClose { subscription.remove() }
    }
    
    suspend fun createAppointment(appointment: Appointment): Result<Unit> {
        return try {
            val docRef = firestore.collection("appointments").document()
            // Blinda o fuso: garante que a hora salva seja exatamente a escolhida
            val finalAppt = appointment.copy(id = docRef.id)
            firestore.collection("appointments").document(docRef.id).set(finalAppt).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getClientHistory(clientUid: String): List<Appointment> {
        return try {
            val snapshot = firestore.collection("appointments")
                .whereEqualTo("clientUid", clientUid)
                .get().await()
            snapshot.toObjects(Appointment::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAppointmentById(id: String): Appointment? = try {
        val snapshot = firestore.collection("appointments").document(id).get().await()
        snapshot.toObject(Appointment::class.java)
    } catch (e: Exception) {
        null
    }

    suspend fun notifyWaitlistForFreeSlot(appt: Appointment) {
        val apptDate = appt.date ?: return
        val startOfDay = Calendar.getInstance().apply {
            time = apptDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        
        val endOfDay = Calendar.getInstance().apply {
            time = apptDate
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.time

        try {
            val snapshot = firestore.collection("waiting_list")
                .whereEqualTo("salonId", appt.salonId)
                .whereEqualTo("status", "WAITING")
                .whereGreaterThanOrEqualTo("date", startOfDay)
                .whereLessThanOrEqualTo("date", endOfDay)
                .get().await()

            val waitlist = snapshot.toObjects(com.example.agendei_pro.core.model.WaitingEntry::class.java)
            if (waitlist.isEmpty()) return

            val salonSnap = firestore.collection("salons").document(appt.salonId).get().await()
            val salonName = salonSnap.getString("name") ?: "Salão"

            val sdf = java.text.SimpleDateFormat("dd/MM", java.util.Locale.US)
            val dateStr = sdf.format(apptDate)

            for (entry in waitlist) {
                if (entry.professionalId != "ANY" && entry.professionalId != appt.professionalId) {
                    continue
                }

                val notificationDoc = firestore.collection("notifications").document()
                val notificationData = mapOf(
                    "id" to notificationDoc.id,
                    "recipientUid" to entry.clientUid,
                    "title" to "Vaga Liberada! 📅",
                    "message" to "Um horário foi liberado no $salonName para o dia $dateStr. Abra o aplicativo para agendar!",
                    "createdAt" to Date()
                )
                notificationDoc.set(notificationData).await()
            }
        } catch (e: Exception) {}
    }

    suspend fun updateAppointmentStatus(id: String, status: String, validateLoyalty: Boolean = false): Result<Unit> = try {
        val appt = getAppointmentById(id)
        val updates = mutableMapOf<String, Any>("status" to status)
        if (validateLoyalty) {
            updates["loyaltyValidated"] = true
        }
        if (status == "CANCELLED") {
            val user = auth.currentUser
            val cancelledBy = if (user != null && user.uid == appt?.clientUid) "CLIENT" else "SALON"
            updates["cancelledBy"] = cancelledBy
        }
        firestore.collection("appointments").document(id).update(updates).await()
        if (status == "CANCELLED" && appt != null && appt.status != "CANCELLED") {
            notifyWaitlistForFreeSlot(appt)
            
            // Cria notificação na coleção 'notifications' para notificar o outro lado
            val user = auth.currentUser
            val recipientUid = if (user != null && user.uid == appt.clientUid) {
                appt.salonId // Notifica o salão
            } else {
                appt.clientUid // Notifica o cliente
            }
            
            val sdf = java.text.SimpleDateFormat("dd/MM 'às' HH:mm", java.util.Locale.getDefault())
            val dateStr = appt.date?.let { sdf.format(it) } ?: ""
            val title = "Agendamento Cancelado ❌"
            val message = if (user != null && user.uid == appt.clientUid) {
                "O cliente ${appt.clientName} cancelou o horário de $dateStr (${appt.serviceName})."
            } else {
                val salonSnap = firestore.collection("salons").document(appt.salonId).get().await()
                val salonName = salonSnap.getString("name") ?: "O salão"
                "$salonName cancelou seu horário de $dateStr para ${appt.serviceName}."
            }

            val notificationDoc = firestore.collection("notifications").document()
            val notificationData = mapOf(
                "id" to notificationDoc.id,
                "recipientUid" to recipientUid,
                "title" to title,
                "message" to message,
                "createdAt" to Date()
            )
            notificationDoc.set(notificationData).await()
        }
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    suspend fun updateLoyaltyValidation(id: String, validated: Boolean): Result<Unit> = try {
        firestore.collection("appointments").document(id).update("loyaltyValidated", validated).await()
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    suspend fun deleteAppointment(id: String): Result<Unit> = try {
        val appt = getAppointmentById(id)
        firestore.collection("appointments").document(id).delete().await()
        if (appt != null && appt.status != "CANCELLED") {
            notifyWaitlistForFreeSlot(appt)
        }
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    suspend fun redeemLoyaltyRewards(clientUid: String, salonId: String, limit: Int, professionalId: String? = null): Result<Unit> = try {
        var query = firestore.collection("appointments")
            .whereEqualTo("clientUid", clientUid)
            .whereEqualTo("salonId", salonId)
            .whereEqualTo("status", "CONFIRMED")
            .whereEqualTo("loyaltyValidated", true)
            
        if (professionalId != null) {
            query = query.whereEqualTo("professionalId", professionalId)
        }
        
        val snapshots = query.get().await()
        
        val unredeemedAppts = snapshots.documents
            .filter { !(it.getBoolean("loyaltyRedeemed") ?: false) }
            .sortedBy { it.getDate("date") ?: Date(0) }
            .take(limit)
            
        if (unredeemedAppts.isEmpty()) {
            throw Exception("Nenhum selo ativo disponível para resgate")
        }

        val batch = firestore.batch()
        for (doc in unredeemedAppts) {
            batch.update(doc.reference, "loyaltyRedeemed", true)
        }
        batch.commit().await()
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }
}
