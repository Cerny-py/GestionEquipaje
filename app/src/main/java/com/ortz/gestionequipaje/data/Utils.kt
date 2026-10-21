package com.ortz.gestionequipaje.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth

object Eventos {
    fun registrar(context: Context, evento: String, tipoEntidad: String) {
        val bundle = Bundle().apply { putString("tipo_entidad", tipoEntidad) }
        FirebaseAnalytics.getInstance(context).logEvent(evento, bundle)
    }
}

object Red {
    fun hayInternet(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val red = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(red) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

object Sesion {
    fun uid(): String {
        return FirebaseAuth.getInstance().currentUser?.uid ?: ""
    }
}