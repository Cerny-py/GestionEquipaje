package com.ortz.gestionequipaje.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object DriveHelper {

    private const val NOMBRE_CARPETA = "GestionEquipaje"

    // Busca la carpeta (o la crea) y sube el PDF. Devuelve el id del archivo en Drive.
    // OJO: bloquea hasta terminar, así que solo se llama dentro de un Thread.
    fun subirComprobante(token: String, pdf: File): String {
        val carpetaId = buscarOCrearCarpeta(token)
        return subirArchivo(token, carpetaId, pdf)
    }

    private fun abrir(direccion: String, metodo: String, token: String): HttpURLConnection {
        val con = URL(direccion).openConnection() as HttpURLConnection
        con.requestMethod = metodo
        con.connectTimeout = 15000
        con.readTimeout = 30000
        con.setRequestProperty("Authorization", "Bearer $token")
        return con
    }

    private fun leer(con: HttpURLConnection): String {
        val codigo = con.responseCode
        val flujo = if (codigo in 200..299) con.inputStream else con.errorStream
        val texto = flujo?.bufferedReader()?.use { it.readText() } ?: ""
        con.disconnect()
        if (codigo !in 200..299) {
            throw Exception("Drive respondió $codigo: $texto")
        }
        return texto
    }

    private fun buscarOCrearCarpeta(token: String): String {
        // 1. Buscar si la carpeta ya existe
        val consulta = "name='$NOMBRE_CARPETA' and mimeType='application/vnd.google-apps.folder' and trashed=false"
        val direccion = "https://www.googleapis.com/drive/v3/files?q=" +
                URLEncoder.encode(consulta, "UTF-8") + "&fields=files(id)"
        val busqueda = abrir(direccion, "GET", token)
        val archivos = JSONObject(leer(busqueda)).getJSONArray("files")
        if (archivos.length() > 0) {
            return archivos.getJSONObject(0).getString("id")
        }

        // 2. No existe: crearla
        val crear = abrir("https://www.googleapis.com/drive/v3/files", "POST", token)
        crear.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        crear.doOutput = true
        val datos = JSONObject()
        datos.put("name", NOMBRE_CARPETA)
        datos.put("mimeType", "application/vnd.google-apps.folder")
        crear.outputStream.use { it.write(datos.toString().toByteArray()) }
        return JSONObject(leer(crear)).getString("id")
    }

    private fun subirArchivo(token: String, carpetaId: String, pdf: File): String {
        val limite = "limite_gestion_equipaje"

        val metadatos = JSONObject()
        metadatos.put("name", pdf.name)
        metadatos.put("parents", JSONArray().put(carpetaId))

        val cuerpo = ByteArrayOutputStream()
        cuerpo.write("--$limite\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n".toByteArray())
        cuerpo.write(metadatos.toString().toByteArray())
        cuerpo.write("\r\n--$limite\r\nContent-Type: application/pdf\r\n\r\n".toByteArray())
        cuerpo.write(pdf.readBytes())
        cuerpo.write("\r\n--$limite--".toByteArray())

        val con = abrir(
            "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id",
            "POST", token
        )
        con.setRequestProperty("Content-Type", "multipart/related; boundary=$limite")
        con.doOutput = true
        con.outputStream.use { it.write(cuerpo.toByteArray()) }
        return JSONObject(leer(con)).getString("id")
    }
}

