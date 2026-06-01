package com.example.agendei_pro.core.service

import com.example.agendei_pro.MainActivity
import com.example.agendei_pro.showLocalNotification
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Quando chega uma mensagem do FCM
        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "Agendei"
        val message = remoteMessage.notification?.body ?: remoteMessage.data["message"] ?: "Nova atualização no seu agendamento"

        // Mostra a notificação usando a função que já existe na MainActivity
        showLocalNotification(applicationContext, title, message)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Se o token mudar, o MainViewModel já cuida de salvar no próximo checkStatus, 
        // mas o ideal seria salvar aqui também se o usuário estivesse logado.
    }
}
