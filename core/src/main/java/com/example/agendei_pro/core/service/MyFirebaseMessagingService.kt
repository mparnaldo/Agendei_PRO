package com.example.agendei_pro.core.service

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.launch
import com.example.agendei_pro.core.service.showLocalNotification

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Quando chega uma mensagem do FCM
        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "Agendei"
        val message = remoteMessage.notification?.body ?: remoteMessage.data["message"] ?: "Nova atualização no seu agendamento"

        // Mostra a notificação usando a função utilitária local do core
        showLocalNotification(applicationContext, title, message)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val profileRepository = com.example.agendei_pro.core.repository.ProfileRepository()
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                profileRepository.updateFcmToken(token)
            } catch (e: Exception) {
                // Falha silenciosa se não autenticado
            }
        }
    }
}
