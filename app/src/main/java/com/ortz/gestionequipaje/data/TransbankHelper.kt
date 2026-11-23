package com.ortz.gestionequipaje.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object TransbankHelper {

    data class InicioPago(val token: String, val url: String)
    data class ResultadoPago(val aprobado: Boolean, val estado: String, val codigoRespuesta: Int)

    fun crearTransaccion(buyOrder: String, sessionId: String, monto: Long): InicioPago {
        val body = JSONObject().apply {
            put("buy_order", buyOrder)
            put("session_id", sessionId)
            put("amount", monto)
            put("return_url", TransbankConfig.RETURN_URL)
        }
        val conexion = abrir(
            "${TransbankConfig.BASE_URL}/rswebpaytransaction/api/webpay/v1.2/transactions",
            "POST"
        )
        conexion.outputStream.use { it.write(body.toString().toByteArray()) }
        val json = JSONObject(leer(conexion))
        return InicioPago(token = json.getString("token"), url = json.getString("url"))
    }

    fun confirmarTransaccion(token: String): ResultadoPago {
        val conexion = abrir(
            "${TransbankConfig.BASE_URL}/rswebpaytransaction/api/webpay/v1.2/transactions/$token",
            "PUT"
        )
        conexion.outputStream.use { it.write("{}".toByteArray()) }
        val json = JSONObject(leer(conexion))
        val estado = json.optString("status", "")
        val codigo = json.optInt("response_code", -1)
        return ResultadoPago(aprobado = (estado == "AUTHORIZED" && codigo == 0), estado = estado, codigoRespuesta = codigo)
    }

    private fun abrir(url: String, metodo: String): HttpURLConnection {
        val conexion = URL(url).openConnection() as HttpURLConnection
        conexion.requestMethod = metodo
        conexion.doOutput = true
        conexion.connectTimeout = 15000
        conexion.readTimeout = 15000
        conexion.setRequestProperty("Content-Type", "application/json")
        conexion.setRequestProperty("Tbk-Api-Key-Id", TransbankConfig.COMMERCE_CODE)
        conexion.setRequestProperty("Tbk-Api-Key-Secret", TransbankConfig.API_KEY)
        return conexion
    }

    private fun leer(conexion: HttpURLConnection): String {
        val codigo = conexion.responseCode
        val stream = if (codigo in 200..299) conexion.inputStream else conexion.errorStream
        val texto = stream.bufferedReader().use { it.readText() }
        if (codigo !in 200..299) throw Exception("Transbank respondió $codigo: $texto")
        return texto
    }
}