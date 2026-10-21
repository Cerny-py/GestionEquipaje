package com.ortz.gestionequipaje.data

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class ServicioMensajes : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        Log.d("FCM_TOKEN", token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val titulo = message.notification?.title ?: message.data["titulo"] ?: "Gestión de equipaje"
        val contenido = message.notification?.body ?: message.data["mensaje"] ?: ""
        Notificaciones.mostrar(this, titulo, contenido)
    }
}