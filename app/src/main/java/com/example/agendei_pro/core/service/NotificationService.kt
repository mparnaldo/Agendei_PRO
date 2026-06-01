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
import com.example.agendei_pro.BuildConfig
import com.example.agendei_pro.MainActivity
import com.example.agendei_pro.core.model.Appointment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class NotificationService : Service() {

    private var listenerRegistration: ListenerRegistration? = null
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
        val isProVersion = try { BuildConfig.FLAVOR == "pro" } catch (e: Exception) { true }
        val persistentChannelId = "notification_service_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                persistentChannelId,
                "Serviço de Notificações em Segundo Plano",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mantém a sincronização de agendamentos em tempo real ativa"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            999,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val text = if (isProVersion) {
            "Buscando novos agendamentos do salão..."
        } else {
            "Buscando atualizações de seus agendamentos..."
        }

        val notification = NotificationCompat.Builder(this, persistentChannelId)
            .setContentTitle(if (isProVersion) "Agendei PRO" else "Agendei")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)
    }

    private fun setupFirestoreListener(userId: String) {
        listenerRegistration?.remove()

        val isProVersion = try { BuildConfig.FLAVOR == "pro" } catch (e: Exception) { true }

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
                        val newAppts = currentAppts.filter { it.id !in seenAppointmentIds && it.status == "PENDING" }
                        for (appt in newAppts) {
                            showNotification(
                                "Novo Agendamento 📅",
                                "${appt.clientName} agendou ${appt.serviceName}"
                            )
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
                            if (prevStatus != null && prevStatus != "CONFIRMED" && appt.status == "CONFIRMED") {
                                fetchSalonNameAndNotify(appt)
                            }
                            appointmentStatuses[appt.id] = appt.status
                        }
                        val currentIds = currentAppts.map { it.id }.toSet()
                        appointmentStatuses.keys.retainAll(currentIds)
                    }
                }
        }
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
        val channelId = "agendamentos_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Notificações de Agendamentos",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificações de novos agendamentos e confirmações"
                enableLights(true)
                enableVibration(true)
                setSound(defaultSoundUri, Notification.AUDIO_ATTRIBUTES_DEFAULT)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    override fun onDestroy() {
        listenerRegistration?.remove()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
