package com.ortz.gestionequipaje.data

import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.Normalizer

data class UbicacionVerificada(
    val latitud: Double,
    val longitud: Double,
    val descripcion: String
)

object Geocodificador {

    fun verificar(direccion: String, comuna: String): UbicacionVerificada? {
        val calle = URLEncoder.encode(direccion, "UTF-8")
        val ciudad = URLEncoder.encode(comuna, "UTF-8")
        val url = URL(
            "https://nominatim.openstreetmap.org/search?format=jsonv2&addressdetails=1&limit=5&countrycodes=cl" +
                    "&street=$calle&city=$ciudad&country=Chile"
        )
        val conexion = url.openConnection() as HttpURLConnection

        try {
            conexion.requestMethod = "GET"
            conexion.connectTimeout = 8000
            conexion.readTimeout = 8000
            conexion.setRequestProperty("User-Agent", "GestionEquipajeApp/1.0 (contacto: bastianegg117@gmail.com)")

            if (conexion.responseCode != 200) return null

            val respuesta = conexion.inputStream.bufferedReader().use { it.readText() }
            val resultados = JSONArray(respuesta)
            val comunaNormal = normalizar(comuna)

            for (i in 0 until resultados.length()) {
                val resultado = resultados.getJSONObject(i)
                val detalle = resultado.optJSONObject("address") ?: continue

                val esChile = detalle.optString("country_code") == "cl"
                val tieneCalle = listOf("road", "pedestrian", "footway", "path").any { detalle.has(it) }
                val enComuna = normalizar(resultado.optString("display_name")).contains(comunaNormal)

                if (esChile && tieneCalle && enComuna) {
                    return UbicacionVerificada(
                        resultado.getString("lat").toDouble(),
                        resultado.getString("lon").toDouble(),
                        resultado.optString("display_name")
                    )
                }
            }
            return null
        } finally {
            conexion.disconnect()
        }
    }

    private fun normalizar(texto: String): String {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}"), "")
            .lowercase()
    }
}