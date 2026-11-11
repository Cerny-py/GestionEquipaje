package com.ortz.gestionequipaje.data

import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object Distancia {

    private const val ORIGEN_LAT = -34.17083
    private const val ORIGEN_LON = -70.74444
    private val guardadas = ConcurrentHashMap<String, Double>()

    fun haversine(lat: Double, lon: Double): Double {
        val radioTierra = 6371.0
        val dLat = Math.toRadians(lat - ORIGEN_LAT)
        val dLon = Math.toRadians(lon - ORIGEN_LON)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(ORIGEN_LAT)) * cos(Math.toRadians(lat)) * sin(dLon / 2).pow(2)
        return 2 * radioTierra * asin(sqrt(a))
    }

    fun buscarKm(comuna: String): Double? {
        val guardada = guardadas[comuna]
        if (guardada != null) return guardada

        val consulta = URLEncoder.encode("$comuna, Chile", "UTF-8")
        val url = URL("https://nominatim.openstreetmap.org/search?format=json&limit=1&countrycodes=cl&q=$consulta")
        val conexion = url.openConnection() as HttpURLConnection

        try {
            conexion.requestMethod = "GET"
            conexion.connectTimeout = 8000
            conexion.readTimeout = 8000
            conexion.setRequestProperty("User-Agent", "GestionEquipajeApp/1.0 (contacto: bastianegg117@gmail.com)")

            if (conexion.responseCode != 200) return null

            val respuesta = conexion.inputStream.bufferedReader().use { it.readText() }
            val resultados = JSONArray(respuesta)
            if (resultados.length() == 0) return null

            val primero = resultados.getJSONObject(0)
            val km = haversine(primero.getString("lat").toDouble(), primero.getString("lon").toDouble())
            guardadas[comuna] = km
            return km
        } finally {
            conexion.disconnect()
        }
    }
}