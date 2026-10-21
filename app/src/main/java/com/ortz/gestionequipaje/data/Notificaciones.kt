package com.ortz.gestionequipaje.data

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ortz.gestionequipaje.MainActivity

object Notificaciones {
    const val CANAL_ID = "canal_equipaje"

    fun crearCanal(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CANAL_ID, "Avisos de equipaje", NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Notificaciones sobre equipaje y encomiendas" }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
        }
    }

    @SuppressLint("MissingPermission")
    fun mostrar(context: Context, titulo: String, contenido: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificacion = NotificationCompat.Builder(context, CANAL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(titulo)
            .setContentText(contenido)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contenido))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notificacion)
    }
}