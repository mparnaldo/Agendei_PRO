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

    suspend fun updateAppointmentStatus(id: String, status: String): Result<Unit> = try {
        firestore.collection("appointments").document(id).update("status", status).await()
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    suspend fun deleteAppointment(id: String): Result<Unit> = try {
        firestore.collection("appointments").document(id).delete().await()
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }
}
