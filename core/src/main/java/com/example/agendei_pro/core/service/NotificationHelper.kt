package com.example.agendei_pro.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat

/**
 * Função utilitária para exibir notificações locais sem depender de referências estáticas ou 
 * acopladas de atividades do módulo principal (evitando dependências circulares com a MainActivity).
 */
fun showLocalNotification(context: Context, title: String, message: String) {
    val channelId = "agendamentos_channel"
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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

    // Resolve o Intent de lançamento dinamicamente com base no package name da aplicação em execução
    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    val pendingIntent = intent?.let {
        PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            it,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    val notificationBuilder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(title)
        .setContentText(message)
        .setAutoCancel(true)
        .setSound(defaultSoundUri)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setDefaults(NotificationCompat.DEFAULT_ALL)
    
    if (pendingIntent != null) {
        notificationBuilder.setContentIntent(pendingIntent)
    }

    notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
}
