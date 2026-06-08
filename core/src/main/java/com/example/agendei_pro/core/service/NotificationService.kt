package com.example.agendei_pro.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.agendei_pro.core.model.Appointment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class NotificationService : Service() {

    private var listenerRegistration: ListenerRegistration? = null
    private val activeRegistrations = mutableListOf<ListenerRegistration>()
    private val salonPromoRegistrations = mutableMapOf<String, ListenerRegistration>()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val user = auth.currentUser
        if (user == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        setupFirestoreListener(user.uid)
        return START_STICKY
    }

    private fun startForegroundService() {
        // Identifica se a aplicação em execução é a versão PRO usando o package name
        val isProVersion = packageName == "com.example.agendei_pro"
        val persistentChannelId = "notification_service_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                persistentChannelId,
                "Serviço de Notificações em Segundo Plano",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mantém a sincronização de agendamentos em tempo real activa"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        // Obtém dinamicamente o Intent de inicialização para evitar dependência de MainActivity::class.java
        val notificationIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = notificationIntent?.let {
            PendingIntent.getActivity(
                this,
                999,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val text = if (isProVersion) {
            "Buscando novos agendamentos do salão..."
        } else {
            "Buscando atualizações de seus agendamentos..."
        }

        val notificationBuilder = NotificationCompat.Builder(this, persistentChannelId)
            .setContentTitle(if (isProVersion) "Agendei PRO" else "Agendei")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)

        if (pendingIntent != null) {
            notificationBuilder.setContentIntent(pendingIntent)
        }

        startForeground(1, notificationBuilder.build())
    }

    private fun setupFirestoreListener(userId: String) {
        listenerRegistration?.remove()
        activeRegistrations.forEach { it.remove() }
        activeRegistrations.clear()
        salonPromoRegistrations.values.forEach { it.remove() }
        salonPromoRegistrations.clear()

        val isProVersion = packageName == "com.example.agendei_pro"

        var initialLoad = true

        if (isProVersion) {
            val seenAppointmentIds = mutableSetOf<String>()
            listenerRegistration = firestore.collection("appointments")
                .whereEqualTo("salonId", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshot == null) return@addSnapshotListener

                    val currentAppts = snapshot.toObjects(Appointment::class.java)
                    val currentIds = currentAppts.map { it.id }.toSet()

                    if (initialLoad) {
                        seenAppointmentIds.addAll(currentIds)
                        initialLoad = false
                    } else {
                        val newAppts = currentAppts.filter { it.id !in seenAppointmentIds }
                        for (appt in newAppts) {
                            val isConfirmed = appt.status == "CONFIRMED"
                            val title = if (isConfirmed) "Agendamento Automático ✅" else "Novo Agendamento 📅"
                            val timeStr = if (appt.date != null) {
                                val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.US)
                                sdf.format(appt.date)
                            } else ""
                            val body = if (isConfirmed) 
                                "Novo agendamento com ${appt.clientName} às $timeStr (${appt.professionalName}) - Confirmado" 
                                else "Novo agendamento com ${appt.clientName} às $timeStr (${appt.professionalName})"
                            showNotification(title, body)
                        }
                        seenAppointmentIds.clear()
                        seenAppointmentIds.addAll(currentIds)
                    }
                }
        } else {
            val appointmentStatuses = mutableMapOf<String, String>()
            listenerRegistration = firestore.collection("appointments")
                .whereEqualTo("clientUid", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshot == null) return@addSnapshotListener

                    val currentAppts = snapshot.toObjects(Appointment::class.java)

                    if (initialLoad) {
                        for (appt in currentAppts) {
                            appointmentStatuses[appt.id] = appt.status
                        }
                        initialLoad = false
                    } else {
                        for (appt in currentAppts) {
                            val prevStatus = appointmentStatuses[appt.id]
                            if (prevStatus == null) {
                                if (appt.status == "CONFIRMED") {
                                    fetchSalonNameAndNotify(appt)
                                }
                            } else if (prevStatus != "CONFIRMED" && appt.status == "CONFIRMED") {
                                fetchSalonNameAndNotify(appt)
                            }
                            appointmentStatuses[appt.id] = appt.status
                        }
                        val currentIds = currentAppts.map { it.id }.toSet()
                        appointmentStatuses.keys.retainAll(currentIds)
                    }
                }

            // Client-side dynamically registered listeners for salon promotions
            val clientBindingsReg = firestore.collection("user_bindings")
                .whereEqualTo("userId", userId)
                .addSnapshotListener { bindingsSnapshot, error ->
                    if (error != null || bindingsSnapshot == null) return@addSnapshotListener
                    
                    val activeSalonIds = bindingsSnapshot.documents.mapNotNull { it.getString("salonId") }.toSet()
                    
                    val toRemove = salonPromoRegistrations.keys.filter { it !in activeSalonIds }
                    for (sid in toRemove) {
                        salonPromoRegistrations[sid]?.remove()
                        salonPromoRegistrations.remove(sid)
                    }
                    
                    for (sid in activeSalonIds) {
                        if (sid !in salonPromoRegistrations) {
                            var initialPromoLoad = true
                            val seenPromoIds = mutableSetOf<String>()
                            
                            val promoReg = firestore.collection("salons").document(sid)
                                .collection("promotions")
                                .addSnapshotListener { promoSnapshot, promoError ->
                                    if (promoError != null || promoSnapshot == null) return@addSnapshotListener
                                    
                                    val promoIds = promoSnapshot.documents.mapNotNull { it.id }.toSet()
                                    
                                    if (initialPromoLoad) {
                                        seenPromoIds.addAll(promoIds)
                                        initialPromoLoad = false
                                    } else {
                                        val newPromos = promoSnapshot.documents.filter { it.id !in seenPromoIds }
                                        for (doc in newPromos) {
                                            val title = doc.getString("title") ?: "Novidade no Salão!"
                                            val msg = doc.getString("message") ?: ""
                                            showNotification(title, msg)
                                        }
                                        seenPromoIds.clear()
                                        seenPromoIds.addAll(promoIds)
                                    }
                                }
                            salonPromoRegistrations[sid] = promoReg
                        }
                    }
                }
            val notificationsReg = firestore.collection("notifications")
                .whereEqualTo("recipientUid", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    val documents = snapshot.documents
                    for (doc in documents) {
                        val title = doc.getString("title") ?: "Vaga Liberada!"
                        val message = doc.getString("message") ?: ""
                        showNotification(title, message)
                        doc.reference.delete()
                    }
                }
            activeRegistrations.add(notificationsReg)
            activeRegistrations.add(clientBindingsReg)
        }

        // Shared background listeners for Admin Broadcast notifications
        var initialBroadcastLoad = true
        val broadcastConfigName = if (isProVersion) {
            "announcement_salons"
        } else {
            "announcement_clients"
        }
        
        val regBroadcast = firestore.collection("config").document(broadcastConfigName)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val id = snapshot.getString("id") ?: return@addSnapshotListener
                val title = snapshot.getString("title") ?: "Aviso"
                val msg = snapshot.getString("message") ?: ""
                
                val prefs = getSharedPreferences("notification_service_prefs", Context.MODE_PRIVATE)
                val lastSeenId = prefs.getString("last_seen_broadcast_id", null)
                
                if (initialBroadcastLoad) {
                    initialBroadcastLoad = false
                    if (lastSeenId == null) {
                        prefs.edit().putString("last_seen_broadcast_id", id).apply()
                    }
                } else {
                    if (id != lastSeenId) {
                        prefs.edit().putString("last_seen_broadcast_id", id).apply()
                        showNotification(title, msg)
                    }
                }
            }
        activeRegistrations.add(regBroadcast)
    }

    private fun fetchSalonNameAndNotify(appt: Appointment) {
        firestore.collection("salons").document(appt.salonId).get()
            .addOnSuccessListener { document ->
                val salonName = document.getString("name") ?: "O salão"
                showNotification(
                    "Agendamento Aceito! ✅",
                    "$salonName confirmou seu horário de ${appt.serviceName}"
                )
            }
            .addOnFailureListener {
                showNotification(
                    "Agendamento Aceito! ✅",
                    "Seu horário de ${appt.serviceName} foi confirmado"
                )
            }
    }

    private fun showNotification(title: String, message: String) {
        // Delega para o utilitário compartilhado que obtém a MainActivity dinamicamente
        showLocalNotification(this, title, message)
    }

    override fun onDestroy() {
        listenerRegistration?.remove()
        activeRegistrations.forEach { it.remove() }
        activeRegistrations.clear()
        salonPromoRegistrations.values.forEach { it.remove() }
        salonPromoRegistrations.clear()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
